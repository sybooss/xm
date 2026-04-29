package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.*;
import com.user.returnsassistant.pojo.*;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.ChatService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;
    @Autowired
    private IntentRecordMapper intentRecordMapper;
    @Autowired
    private RetrievalLogMapper retrievalLogMapper;
    @Autowired
    private AiCallLogMapper aiCallLogMapper;
    @Autowired
    private ProcessTraceMapper processTraceMapper;
    @Autowired
    private AiService aiService;

    @Override
    public PageResult<ChatSession> page(ChatSessionSearch search) {
        return new PageResult<>(sessionMapper.count(search), sessionMapper.page(search));
    }

    @Override
    public ChatSession save(ChatSession session) {
        if (session.getSessionNo() == null || session.getSessionNo().isBlank()) {
            session.setSessionNo(NoUtils.sessionNo());
        }
        if (session.getTitle() == null || session.getTitle().isBlank()) {
            session.setTitle("新客服会话");
        }
        if (session.getOrderNo() != null && !session.getOrderNo().isBlank()) {
            DemoOrder order = orderMapper.getByOrderNo(session.getOrderNo());
            if (order != null) {
                session.setOrderId(order.getId());
            }
        }
        session.setStatus("ACTIVE");
        session.setChannel(session.getChannel() == null ? "WEB" : session.getChannel());
        sessionMapper.insert(session);
        return session;
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        ChatSession session = requireSession(id);
        if (session.getOrderId() != null) {
            session.setOrder(orderMapper.getById(session.getOrderId()));
        }
        session.setMessages(messageMapper.listBySessionId(id));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", session.getId());
        data.put("sessionNo", session.getSessionNo());
        data.put("title", session.getTitle());
        data.put("status", session.getStatus());
        data.put("currentIntent", session.getCurrentIntent());
        data.put("summary", session.getSummary());
        data.put("order", session.getOrder());
        data.put("messages", session.getMessages());
        return data;
    }

    @Override
    public void update(Long id, ChatSession session) {
        ChatSession old = requireSession(id);
        session.setId(id);
        session.setSessionNo(old.getSessionNo());
        if (session.getOrderNo() != null && !session.getOrderNo().isBlank()) {
            DemoOrder order = orderMapper.getByOrderNo(session.getOrderNo());
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            session.setOrderId(order.getId());
        } else {
            session.setOrderId(old.getOrderId());
        }
        if (session.getTitle() == null) {
            session.setTitle(old.getTitle());
        }
        if (session.getStatus() == null) {
            session.setStatus(old.getStatus());
        }
        sessionMapper.update(session);
    }

    @Override
    public void delete(Long id) {
        sessionMapper.delete(id);
    }

    @Override
    public List<ChatMessage> listMessages(Long id) {
        requireSession(id);
        return messageMapper.listBySessionId(id);
    }

    @Transactional
    @Override
    public Map<String, Object> sendMessage(Long id, ChatMessageRequest request) {
        ChatSession session = requireSession(id);
        if ("CLOSED".equals(session.getStatus())) {
            throw new BusinessException("会话已关闭");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        DemoOrder order = resolveOrder(session, request.getOrderNo());
        int nextSeq = messageMapper.maxSeqNo(id) + 1;
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(id);
        userMessage.setRole("USER");
        userMessage.setContent(request.getContent());
        userMessage.setMessageType("TEXT");
        userMessage.setSeqNo(nextSeq);
        messageMapper.insert(userMessage);

        IntentRecord intent = recognizeIntent(id, userMessage.getId(), request.getContent(), request.getOrderNo());
        trace(id, userMessage.getId(), "INTENT_RECOGNIZE", "SUCCESS", "{\"intentCode\":\"" + intent.getIntentCode() + "\"}");

        Map<String, Object> orderContext = buildOrderContext(order);
        trace(id, userMessage.getId(), "ORDER_CONTEXT", order == null ? "SKIPPED" : "SUCCESS", "{\"hasOrder\":" + (order != null) + "}");

        List<KnowledgeDoc> hits = knowledgeDocMapper.search(request.getContent(), intent.getIntentCode(), 5);
        int rank = 1;
        for (KnowledgeDoc doc : hits) {
            RetrievalLog log = new RetrievalLog();
            log.setSessionId(id);
            log.setMessageId(userMessage.getId());
            log.setQueryText(request.getContent());
            log.setDocId(doc.getId());
            log.setRankNo(rank++);
            log.setScore(BigDecimal.valueOf(doc.getScore() == null ? 0.8 : doc.getScore()).setScale(4, RoundingMode.HALF_UP));
            log.setHitReason(doc.getHitReason());
            log.setDocTitleSnapshot(doc.getTitle());
            log.setDocContentSnapshot(doc.getContent());
            retrievalLogMapper.insert(log);
        }
        trace(id, userMessage.getId(), "KNOWLEDGE_RETRIEVAL", hits.isEmpty() ? "SKIPPED" : "SUCCESS", "{\"hitCount\":" + hits.size() + "}");

        String localReply = buildReply(intent, order, hits);
        String aiPrompt = buildAiPrompt(request.getContent(), intent, orderContext, hits, localReply);
        boolean useAi = request.getUseAi() == null || request.getUseAi();
        AiService.AiResult aiResult = useAi ? aiService.generate(aiPrompt) : new AiService.AiResult(false, "SKIPPED", true, "local-fallback", "local-rule-template", "", 0, "本轮未启用 AI");
        aiCallLogMapper.insert(aiService.toLog(id, userMessage.getId(), aiPrompt, aiResult));
        trace(id, userMessage.getId(), "AI_GENERATION", aiResult.status(), "{\"fallbackUsed\":" + aiResult.fallbackUsed() + "}");

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(id);
        assistantMessage.setRole("ASSISTANT");
        boolean enhanced = aiResult.used() && aiResult.reply() != null && !aiResult.reply().isBlank();
        assistantMessage.setContent(enhanced ? aiResult.reply() : localReply);
        assistantMessage.setMessageType("TEXT");
        assistantMessage.setSeqNo(nextSeq + 1);
        assistantMessage.setReplyToId(userMessage.getId());
        assistantMessage.setIntentCode(intent.getIntentCode());
        assistantMessage.setSourceType(enhanced ? "AI_ENHANCED" : "FALLBACK");
        messageMapper.insert(assistantMessage);
        trace(id, userMessage.getId(), "FINAL_REPLY", "SUCCESS", "{\"sourceType\":\"" + assistantMessage.getSourceType() + "\"}");

        sessionMapper.updateSummary(id, intent.getIntentCode(), "最近咨询：" + trim(request.getContent(), 80));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userMessage", userMessage);
        data.put("assistantMessage", assistantMessage);
        data.put("intent", intent);
        data.put("orderContext", orderContext);
        data.put("knowledgeHits", hits);
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("used", aiResult.used());
        ai.put("status", aiResult.status());
        ai.put("provider", aiResult.provider());
        ai.put("modelName", aiResult.modelName());
        ai.put("fallbackUsed", aiResult.fallbackUsed());
        ai.put("latencyMs", aiResult.latencyMs());
        ai.put("errorMessage", aiResult.errorMessage());
        data.put("ai", ai);
        data.put("trace", processTraceMapper.listBySessionId(id));
        data.put("suggestedQuestions", suggestedQuestions(intent.getIntentCode()));
        return data;
    }

    @Override
    public List<ProcessTrace> listTraces(Long id) {
        requireSession(id);
        return processTraceMapper.listBySessionId(id);
    }

    private ChatSession requireSession(Long id) {
        ChatSession session = sessionMapper.getById(id);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }

    private DemoOrder resolveOrder(ChatSession session, String orderNo) {
        if (orderNo != null && !orderNo.isBlank()) {
            DemoOrder order = orderMapper.getByOrderNo(orderNo);
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            sessionMapper.bindOrder(session.getId(), order.getId());
            return order;
        }
        if (session.getOrderId() != null) {
            return orderMapper.getById(session.getOrderId());
        }
        return null;
    }

    private IntentRecord recognizeIntent(Long sessionId, Long messageId, String content, String orderNo) {
        String text = content == null ? "" : content;
        String code = "RULE_EXPLAIN";
        String name = "规则说明";
        if (containsAny(text, "换货", "换一个", "更换")) {
            code = "EXCHANGE_APPLY";
            name = "换货申请";
        } else if (containsAny(text, "退货", "退掉", "不要了", "能不能退")) {
            code = "RETURN_APPLY";
            name = "退货申请";
        } else if (containsAny(text, "退款", "到账", "钱", "退到哪里")) {
            code = "REFUND_PROGRESS";
            name = "退款进度";
        } else if (containsAny(text, "物流", "快递", "没动", "不动", "签收")) {
            code = "LOGISTICS_QUERY";
            name = "物流查询";
        } else if (containsAny(text, "投诉", "人工", "客服", "介入")) {
            code = "COMPLAINT_TRANSFER";
            name = "投诉与人工转接";
        } else if (containsAny(text, "买", "推荐", "咨询")) {
            code = "PRE_SALE";
            name = "售前咨询";
        }
        IntentRecord record = new IntentRecord();
        record.setSessionId(sessionId);
        record.setMessageId(messageId);
        record.setIntentCode(code);
        record.setIntentName(name);
        record.setConfidence(BigDecimal.valueOf(0.88));
        record.setMethod("RULE");
        record.setSlotsJson("{\"orderNo\":" + (orderNo == null ? "null" : "\"" + orderNo + "\"") + "}");
        intentRecordMapper.insert(record);
        return record;
    }

    private Map<String, Object> buildOrderContext(DemoOrder order) {
        if (order == null) {
            return Map.of("hasOrder", false, "tip", "用户暂未提供订单号");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hasOrder", true);
        map.put("orderNo", order.getOrderNo());
        map.put("productName", order.getProductName());
        map.put("orderStatus", order.getOrderStatus());
        map.put("logisticsStatus", order.getLogisticsStatus());
        map.put("afterSaleStatus", order.getAfterSaleStatus());
        map.put("signedDays", signedDays(order));
        return map;
    }

    private String buildReply(IntentRecord intent, DemoOrder order, List<KnowledgeDoc> hits) {
        String basis = hits.isEmpty() ? "当前知识库没有精确命中规则，以下回复基于本地售后流程模板。" : "已参考知识库：" + hits.get(0).getTitle() + "。";
        String orderText = order == null ? "你还没有提供订单号，建议补充订单号后我可以结合订单状态判断。" :
                "当前订单号为 " + order.getOrderNo() + "，订单状态为 " + order.getOrderStatus() + "，售后状态为 " + order.getAfterSaleStatus() + "。";
        return switch (intent.getIntentCode()) {
            case "RETURN_APPLY" -> orderText + basis + " 如商品已签收且仍在规则允许时间内，通常可以在订单详情页提交退货申请，并保持商品完好、配件齐全。";
            case "EXCHANGE_APPLY" -> orderText + basis + " 如果商品存在质量问题或规格不符，可以提交换货申请，并上传问题照片方便商家审核。";
            case "REFUND_PROGRESS" -> orderText + basis + " 退款一般会在商家确认收货或审核通过后按原支付渠道退回，具体到账时间以支付渠道为准。";
            case "LOGISTICS_QUERY" -> orderText + basis + " 如果物流长时间不更新，建议先查看最新物流节点，必要时联系商家或申请平台介入。";
            case "COMPLAINT_TRANSFER" -> orderText + basis + " 如果商家长时间不处理或处理结果不合理，可以发起投诉或转人工客服处理。";
            case "PRE_SALE" -> basis + " 你可以补充商品型号、预算和使用场景，我会按售后规则和商品信息给出咨询建议。";
            default -> orderText + basis + " 请补充更具体的问题，例如退货、换货、退款进度或物流异常。";
        };
    }

    private String buildAiPrompt(String userMessage, IntentRecord intent, Map<String, Object> orderContext, List<KnowledgeDoc> hits, String localReply) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商退换货智能客服系统的回复生成模块。请基于已给出的业务判断、订单上下文和知识库依据，生成一段简洁、可靠、可执行的中文客服回复。\n\n");
        prompt.append("回复要求：\n");
        prompt.append("1. 不要编造平台规则，不要承诺系统没有提供的信息。\n");
        prompt.append("2. 如果订单信息不足，要引导用户补充订单号或必要信息。\n");
        prompt.append("3. 如果知识库依据为空，只能给出通用建议。\n");
        prompt.append("4. 回复适合展示在客服工作台，语气礼貌清楚，控制在 180 字以内。\n\n");
        prompt.append("用户问题：\n").append(nullToEmpty(userMessage)).append("\n\n");
        prompt.append("识别意图：\n")
                .append(intent.getIntentCode()).append(" / ")
                .append(intent.getIntentName()).append("，置信度 ")
                .append(intent.getConfidence()).append("\n\n");
        prompt.append("订单上下文：\n").append(orderContext).append("\n\n");
        prompt.append("知识库命中：\n");
        if (hits.isEmpty()) {
            prompt.append("无精确命中。\n");
        } else {
            int limit = Math.min(3, hits.size());
            for (int i = 0; i < limit; i++) {
                KnowledgeDoc doc = hits.get(i);
                prompt.append(i + 1).append(". ")
                        .append(doc.getTitle()).append("：")
                        .append(trim(firstText(doc.getAnswer(), doc.getContent(), doc.getContentPreview()), 240))
                        .append("\n");
            }
        }
        prompt.append("\n业务规则初步判断：\n").append(localReply).append("\n\n");
        prompt.append("请输出最终客服回复，不要输出分析过程。");
        return prompt.toString();
    }

    private String firstText(String... texts) {
        for (String text : texts) {
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private List<String> suggestedQuestions(String intentCode) {
        return switch (intentCode) {
            case "RETURN_APPLY" -> List.of("退货后多久能退款？", "退货需要自己承担运费吗？");
            case "EXCHANGE_APPLY" -> List.of("换货需要多久？", "换货可以改成退货吗？");
            case "REFUND_PROGRESS" -> List.of("退款会退到哪里？", "退款失败怎么办？");
            case "LOGISTICS_QUERY" -> List.of("物流不更新可以投诉吗？", "包裹丢失怎么办？");
            default -> List.of("能不能帮我查订单？", "我想转人工客服");
        };
    }

    private long signedDays(DemoOrder order) {
        if (order.getSignedAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(order.getSignedAt(), LocalDateTime.now()).toDays());
    }

    private void trace(Long sessionId, Long messageId, String stepName, String stepStatus, String detailJson) {
        ProcessTrace trace = new ProcessTrace();
        trace.setSessionId(sessionId);
        trace.setMessageId(messageId);
        trace.setStepName(stepName);
        trace.setStepStatus(stepStatus);
        trace.setDetailJson(detailJson);
        processTraceMapper.insert(trace);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String trim(String text, int length) {
        if (text == null || text.length() <= length) {
            return text;
        }
        return text.substring(0, length);
    }
}
