package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.*;
import com.user.returnsassistant.pojo.*;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.AiBusinessToolService;
import com.user.returnsassistant.service.ChatService;
import com.user.returnsassistant.service.ServiceTicketService;
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
    private static final ObjectMapper JSON = new ObjectMapper();

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
    @Autowired
    private AiBusinessToolService aiBusinessToolService;
    @Autowired
    private ServiceTicketService ticketService;

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
        List<ChatMessage> recentBefore = messageMapper.listRecentBySessionId(id, 8);
        ConversationContext conversationContext = buildConversationContext(session, recentBefore, request.getContent());

        int nextSeq = messageMapper.maxSeqNo(id) + 1;
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(id);
        userMessage.setRole("USER");
        userMessage.setContent(request.getContent());
        userMessage.setMessageType("TEXT");
        userMessage.setSeqNo(nextSeq);
        messageMapper.insert(userMessage);

        trace(id, userMessage.getId(), "CONTEXT_RESOLVE", conversationContext.followUp() ? "SUCCESS" : "SKIPPED",
                detail("title", "多轮上下文解析",
                        "summary", conversationContext.summary(),
                        "followUp", conversationContext.followUp(),
                        "inheritedIntent", conversationContext.inheritedIntentCode(),
                        "recentMessageCount", recentBefore.size()));

        IntentRecord intent = recognizeIntent(id, userMessage.getId(), request.getContent(), request.getOrderNo(), conversationContext);
        trace(id, userMessage.getId(), "INTENT_RECOGNIZE", "SUCCESS",
                detail("title", "意图识别",
                        "intentCode", intent.getIntentCode(),
                        "intentName", intent.getIntentName(),
                        "method", intent.getMethod(),
                        "confidence", intent.getConfidence(),
                        "followUp", conversationContext.followUp()));

        Map<String, Object> orderContext = buildOrderContext(order);
        trace(id, userMessage.getId(), "ORDER_CONTEXT", order == null ? "SKIPPED" : "SUCCESS",
                detail("title", "订单上下文",
                        "hasOrder", order != null,
                        "orderNo", order == null ? null : order.getOrderNo(),
                        "afterSaleStatus", order == null ? null : order.getAfterSaleStatus(),
                        "logisticsStatus", order == null ? null : order.getLogisticsStatus()));

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
        trace(id, userMessage.getId(), "KNOWLEDGE_RETRIEVAL", hits.isEmpty() ? "SKIPPED" : "SUCCESS",
                detail("title", "知识库检索",
                        "hitCount", hits.size(),
                        "topTitle", hits.isEmpty() ? null : hits.get(0).getTitle(),
                        "summary", hits.isEmpty() ? "未命中精确规则，使用本地流程模板兜底" : "已命中可用于回答的规则依据"));

        String localReply = buildReply(intent, order, hits, conversationContext);
        Map<String, Object> businessTools = buildBusinessToolEvidence(order, request.getContent(), intent);
        trace(id, userMessage.getId(), "BUSINESS_TOOL_CALLS", "SUCCESS",
                detail("title", "LangChain4j 业务工具",
                        "tools", businessTools.get("tools"),
                        "summary", "已把订单查询、知识检索和工单能力封装为可调用工具，并将结果注入模型上下文"));

        String aiPrompt = buildAiPrompt(request.getContent(), intent, orderContext, hits, localReply, conversationContext, businessTools);
        boolean useAi = request.getUseAi() == null || request.getUseAi();
        AiService.AiResult aiResult = useAi
                ? aiService.generate(aiPrompt)
                : new AiService.AiResult(false, "SKIPPED", true, "local-fallback", "local-rule-template", "", 0, "本轮未启用 AI");
        aiCallLogMapper.insert(aiService.toLog(id, userMessage.getId(), aiPrompt, aiResult));
        trace(id, userMessage.getId(), "AI_GENERATION", aiResult.status(),
                detail("title", "AI 回答生成",
                        "provider", aiResult.provider(),
                        "modelName", aiResult.modelName(),
                        "fallbackUsed", aiResult.fallbackUsed(),
                        "latencyMs", aiResult.latencyMs(),
                        "errorMessage", aiResult.errorMessage()));

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

        Map<String, Object> ticketPayload = handleTicketHandoff(session, order, userMessage, assistantMessage, intent);
        trace(id, userMessage.getId(), "FINAL_REPLY", "SUCCESS",
                detail("title", "最终回复",
                        "sourceType", assistantMessage.getSourceType(),
                        "replyLength", assistantMessage.getContent() == null ? 0 : assistantMessage.getContent().length(),
                        "ticketCreated", Boolean.TRUE.equals(ticketPayload.get("created"))));

        sessionMapper.updateSummary(id, intent.getIntentCode(), buildSessionSummary(order, intent, request.getContent(), conversationContext));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userMessage", userMessage);
        data.put("assistantMessage", assistantMessage);
        data.put("intent", intent);
        data.put("context", conversationContext.toResponse(intent));
        data.put("orderContext", orderContext);
        data.put("knowledgeHits", hits);
        data.put("businessTools", businessTools);
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("used", aiResult.used());
        ai.put("status", aiResult.status());
        ai.put("provider", aiResult.provider());
        ai.put("modelName", aiResult.modelName());
        ai.put("fallbackUsed", aiResult.fallbackUsed());
        ai.put("latencyMs", aiResult.latencyMs());
        ai.put("errorMessage", aiResult.errorMessage());
        data.put("ai", ai);
        data.put("ticket", ticketPayload);
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
            session.setOrderId(order.getId());
            session.setOrderNo(order.getOrderNo());
            return order;
        }
        if (session.getOrderId() != null) {
            return orderMapper.getById(session.getOrderId());
        }
        return null;
    }

    private ConversationContext buildConversationContext(ChatSession session, List<ChatMessage> recentMessages, String content) {
        String text = nullToEmpty(content).trim();
        String lastIntent = session.getCurrentIntent();
        String lastUserQuestion = recentMessages.stream()
                .filter(message -> "USER".equals(message.getRole()))
                .reduce((first, second) -> second)
                .map(ChatMessage::getContent)
                .orElse("");
        boolean followUp = !recentMessages.isEmpty() && (
                containsAny(text, "那", "这个", "这单", "它", "刚才", "上面", "继续", "还有", "呢", "怎么办", "需要什么", "多久到账", "多久", "可以吗", "如果")
                        || (text.length() <= 18 && !hasStrongStandaloneIntent(text))
        );
        String inheritedIntent = followUp && hasText(lastIntent) ? lastIntent : null;
        String summary = recentMessages.isEmpty()
                ? "本轮为会话首问"
                : "上一轮意图：" + nullToDash(lastIntent) + "；最近用户问题：" + trim(lastUserQuestion, 60);
        return new ConversationContext(followUp, lastIntent, inheritedIntent, summary, recentMessages);
    }

    private IntentRecord recognizeIntent(Long sessionId, Long messageId, String content, String orderNo, ConversationContext context) {
        String text = content == null ? "" : content;
        IntentDecision decision = directIntent(text);
        boolean usedContext = false;

        if ("RULE_EXPLAIN".equals(decision.code()) && context.followUp() && hasText(context.inheritedIntentCode())) {
            decision = contextualIntent(text, context.inheritedIntentCode());
            usedContext = true;
        } else if (context.followUp() && !"RULE_EXPLAIN".equals(decision.code())) {
            usedContext = true;
        }

        IntentRecord record = new IntentRecord();
        record.setSessionId(sessionId);
        record.setMessageId(messageId);
        record.setIntentCode(decision.code());
        record.setIntentName(intentName(decision.code()));
        record.setConfidence(BigDecimal.valueOf(usedContext ? decision.contextConfidence() : decision.ruleConfidence()));
        record.setMethod(usedContext ? "HYBRID" : "RULE");
        record.setSlotsJson(toJson(detail("orderNo", orderNo,
                "followUp", context.followUp(),
                "inheritedIntent", context.inheritedIntentCode(),
                "lastIntent", context.lastIntentCode())));
        intentRecordMapper.insert(record);
        return record;
    }

    private IntentDecision directIntent(String text) {
        if (containsAny(text, "投诉", "人工", "客服", "介入", "不处理", "没人管", "升级")) {
            return new IntentDecision("COMPLAINT_TRANSFER", 0.91, 0.93);
        }
        if (containsAny(text, "换货", "换一个", "更换", "换大一码", "换小一码")) {
            return new IntentDecision("EXCHANGE_APPLY", 0.89, 0.92);
        }
        if (containsAny(text, "退货", "退掉", "不要了", "能不能退", "七天无理由", "拒收")) {
            return new IntentDecision("RETURN_APPLY", 0.89, 0.92);
        }
        if (containsAny(text, "退款", "到账", "钱", "退到哪里", "退回", "原路", "多久到账")) {
            return new IntentDecision("REFUND_PROGRESS", 0.88, 0.91);
        }
        if (containsAny(text, "物流", "快递", "没动", "不动", "签收", "包裹", "丢件", "催件")) {
            return new IntentDecision("LOGISTICS_QUERY", 0.87, 0.90);
        }
        if (containsAny(text, "买", "推荐", "咨询", "预售")) {
            return new IntentDecision("PRE_SALE", 0.82, 0.86);
        }
        return new IntentDecision("RULE_EXPLAIN", 0.62, 0.78);
    }

    private IntentDecision contextualIntent(String text, String inheritedIntent) {
        if (containsAny(text, "投诉", "人工", "客服", "介入", "不处理", "没人管", "升级")) {
            return new IntentDecision("COMPLAINT_TRANSFER", 0.78, 0.90);
        }
        if (containsAny(text, "到账", "退款", "钱", "退回", "原路") || ("RETURN_APPLY".equals(inheritedIntent) && containsAny(text, "多久", "几天"))) {
            return new IntentDecision("REFUND_PROGRESS", 0.76, 0.88);
        }
        if (containsAny(text, "物流", "快递", "没动", "不动", "签收", "包裹", "催件")) {
            return new IntentDecision("LOGISTICS_QUERY", 0.76, 0.88);
        }
        if (containsAny(text, "材料", "照片", "寄回", "运费", "怎么办", "可以吗", "需要什么")) {
            return new IntentDecision(inheritedIntent, 0.72, 0.84);
        }
        return new IntentDecision(inheritedIntent, 0.70, 0.82);
    }

    private Map<String, Object> buildOrderContext(DemoOrder order) {
        if (order == null) {
            return detail("hasOrder", false, "tip", "用户暂未提供订单号");
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

    private String buildReply(IntentRecord intent, DemoOrder order, List<KnowledgeDoc> hits, ConversationContext context) {
        String contextText = context.followUp() && hasText(context.inheritedIntentCode())
                ? "结合你前面关于“" + intentName(context.inheritedIntentCode()) + "”的追问，"
                : "";
        String basis = hits.isEmpty() ? "当前知识库没有精确命中规则，以下回复基于本地售后流程模板。" : "已参考知识库：" + hits.get(0).getTitle() + "。";
        String orderText = order == null ? "你还没有提供订单号，建议补充订单号后我可以结合订单状态判断。" :
                "当前订单号为 " + order.getOrderNo() + "，订单状态为 " + order.getOrderStatus() + "，售后状态为 " + order.getAfterSaleStatus() + "。";
        return contextText + switch (intent.getIntentCode()) {
            case "RETURN_APPLY" -> orderText + basis + " 如商品已签收且仍在规则允许时间内，通常可以在订单详情页提交退货申请，并保持商品完好、配件齐全。";
            case "EXCHANGE_APPLY" -> orderText + basis + " 如果商品存在质量问题或规格不符，可以提交换货申请，并上传问题照片方便商家审核。";
            case "REFUND_PROGRESS" -> orderText + basis + " 退款一般会在商家确认收货或审核通过后按原支付渠道退回，具体到账时间以支付渠道为准。";
            case "LOGISTICS_QUERY" -> orderText + basis + " 如果物流长时间不更新，建议先查看最新物流节点，必要时联系商家或申请平台介入。";
            case "COMPLAINT_TRANSFER" -> orderText + basis + " 我已将问题整理成人工客服工单，后续客服可以根据订单、意图和对话摘要继续处理。";
            case "PRE_SALE" -> basis + " 你可以补充商品型号、预算和使用场景，我会按售后规则和商品信息给出咨询建议。";
            default -> orderText + basis + " 请补充更具体的问题，例如退货、换货、退款进度或物流异常。";
        };
    }

    private String buildAiPrompt(String userMessage, IntentRecord intent, Map<String, Object> orderContext, List<KnowledgeDoc> hits,
                                 String localReply, ConversationContext context, Map<String, Object> businessTools) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商退换货智能客服系统的回复生成模块。请基于已给出的业务判断、订单上下文、对话上下文和知识库依据，生成一段简洁、可靠、可执行的中文客服回复。\n\n");
        prompt.append("回复要求：\n");
        prompt.append("1. 不要编造平台规则，不要承诺系统没有提供的信息。\n");
        prompt.append("2. 如果订单信息不足，要引导用户补充订单号或必要信息。\n");
        prompt.append("3. 如果知识库依据为空，只能给出通用建议。\n");
        prompt.append("4. 如果本轮是追问，要自然承接上一轮，不要像重新开场。\n");
        prompt.append("5. 回复适合展示在客服工作台，语气礼貌清楚，控制在 180 字以内。\n\n");
        prompt.append("用户本轮问题：\n").append(nullToEmpty(userMessage)).append("\n\n");
        prompt.append("多轮上下文：\n").append(context.summary()).append("\n");
        for (String line : context.recentPromptLines()) {
            prompt.append("- ").append(line).append("\n");
        }
        prompt.append("\n识别意图：\n")
                .append(intent.getIntentCode()).append(" / ")
                .append(intent.getIntentName()).append("，置信度 ")
                .append(intent.getConfidence()).append("，方法 ")
                .append(intent.getMethod()).append("\n\n");
        prompt.append("订单上下文：\n").append(orderContext).append("\n\n");
        prompt.append("LangChain4j 业务工具结果：\n").append(businessTools).append("\n\n");
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

    private Map<String, Object> buildBusinessToolEvidence(DemoOrder order, String question, IntentRecord intent) {
        String orderNo = order == null ? "" : order.getOrderNo();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tools", List.of("queryOrderStatus", "searchAfterSaleKnowledge", "createServiceTicket"));
        data.put("orderStatus", aiBusinessToolService.queryOrderStatus(orderNo));
        data.put("knowledgeEvidence", aiBusinessToolService.searchAfterSaleKnowledge(question, intent.getIntentCode()));
        data.put("ticketTool", "当用户触发投诉、人工客服或异常升级时，业务链路会调用 createServiceTicket 创建或复用工单");
        return data;
    }

    private Map<String, Object> handleTicketHandoff(ChatSession session, DemoOrder order, ChatMessage userMessage,
                                                    ChatMessage assistantMessage, IntentRecord intent) {
        boolean needsTicket = shouldTransferToHuman(userMessage.getContent(), intent, order);
        if (!needsTicket) {
            trace(session.getId(), userMessage.getId(), "HUMAN_TICKET_CHECK", "SKIPPED",
                    detail("title", "人工工单判定",
                            "created", false,
                            "reason", "当前意图可由智能客服直接处理"));
            return detail("created", false, "needed", false, "reason", "当前意图可由智能客服直接处理");
        }

        String summary = buildTicketSummary(order, intent, userMessage.getContent(), assistantMessage.getContent());
        String action = "请人工客服核实订单、物流和商家处理节点，优先联系用户并在工单中记录最终处理结果。";
        ServiceTicketService.TicketResult result = ticketService.createFromSession(
                session, order, userMessage.getId(), intent.getIntentCode(), trim(userMessage.getContent(), 500),
                summary, action, false);
        ServiceTicket ticket = result.ticket();

        trace(session.getId(), userMessage.getId(), "HUMAN_TICKET_CHECK", "SUCCESS",
                detail("title", "人工工单判定",
                        "created", result.created(),
                        "ticketNo", ticket == null ? null : ticket.getTicketNo(),
                        "reason", result.reason()));
        trace(session.getId(), userMessage.getId(), "TICKET_CREATED", result.created() ? "SUCCESS" : "SKIPPED",
                detail("title", result.created() ? "创建人工工单" : "复用已有人工工单",
                        "ticketNo", ticket == null ? null : ticket.getTicketNo(),
                        "priority", ticket == null ? null : ticket.getPriority(),
                        "status", ticket == null ? null : ticket.getStatus()));

        Map<String, Object> payload = detail(
                "created", result.created(),
                "needed", true,
                "reason", result.reason());
        if (ticket != null) {
            payload.put("id", ticket.getId());
            payload.put("ticketNo", ticket.getTicketNo());
            payload.put("priority", ticket.getPriority());
            payload.put("status", ticket.getStatus());
            payload.put("orderNo", ticket.getOrderNo());
            payload.put("suggestedAction", ticket.getSuggestedAction());
        }
        return payload;
    }

    private boolean shouldTransferToHuman(String content, IntentRecord intent, DemoOrder order) {
        String text = nullToEmpty(content);
        if ("COMPLAINT_TRANSFER".equals(intent.getIntentCode())) {
            return true;
        }
        if (containsAny(text, "投诉", "人工", "客服", "介入", "不处理", "没人管", "升级")) {
            return true;
        }
        return order != null && "ABNORMAL".equals(order.getLogisticsStatus()) && containsAny(text, "物流", "快递", "包裹", "丢", "不动", "没动");
    }

    private String buildTicketSummary(DemoOrder order, IntentRecord intent, String question, String reply) {
        String orderInfo = order == null
                ? "未绑定订单"
                : "订单 " + order.getOrderNo() + "，商品 " + order.getProductName() + "，订单状态 " + order.getOrderStatus()
                + "，物流状态 " + order.getLogisticsStatus() + "，售后状态 " + order.getAfterSaleStatus();
        return trim("用户触发人工转接。识别意图：" + intent.getIntentName() + "（" + intent.getIntentCode() + "）。"
                + orderInfo + "。用户问题：" + nullToEmpty(question) + "。智能客服回复：" + nullToEmpty(reply), 1000);
    }

    private String buildSessionSummary(DemoOrder order, IntentRecord intent, String content, ConversationContext context) {
        String orderPart = order == null ? "未绑定订单" : "订单 " + order.getOrderNo();
        String followPart = context.followUp() ? "，本轮为多轮追问" : "";
        return trim(orderPart + "，当前意图：" + intent.getIntentName() + followPart + "；最近问题：" + nullToEmpty(content), 1000);
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

    private String nullToDash(String text) {
        return hasText(text) ? text : "-";
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private List<String> suggestedQuestions(String intentCode) {
        return switch (intentCode) {
            case "RETURN_APPLY" -> List.of("退货后多久能退款？", "退货需要自己承担运费吗？", "退货需要哪些照片？");
            case "EXCHANGE_APPLY" -> List.of("换货需要多久？", "换货可以改成退货吗？", "需要上传什么凭证？");
            case "REFUND_PROGRESS" -> List.of("退款会退到哪里？", "退款失败怎么办？", "能不能催一下退款？");
            case "LOGISTICS_QUERY" -> List.of("物流不更新可以投诉吗？", "包裹丢失怎么办？", "需要转人工核实吗？");
            case "COMPLAINT_TRANSFER" -> List.of("人工客服多久处理？", "还需要补充哪些材料？");
            default -> List.of("能不能帮我查订单？", "我想转人工客服");
        };
    }

    private long signedDays(DemoOrder order) {
        if (order.getSignedAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(order.getSignedAt(), LocalDateTime.now()).toDays());
    }

    private void trace(Long sessionId, Long messageId, String stepName, String stepStatus, Map<String, Object> detail) {
        trace(sessionId, messageId, stepName, stepStatus, toJson(detail));
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

    private Map<String, Object> detail(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"summary\":\"json serialization failed\"}";
        }
    }

    private boolean hasStrongStandaloneIntent(String text) {
        return containsAny(text, "退货", "换货", "退款", "物流", "快递", "投诉", "人工", "预售", "推荐");
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

    private String intentName(String code) {
        return switch (code) {
            case "PRE_SALE" -> "售前咨询";
            case "RETURN_APPLY" -> "退货申请";
            case "EXCHANGE_APPLY" -> "换货申请";
            case "REFUND_PROGRESS" -> "退款进度";
            case "LOGISTICS_QUERY" -> "物流查询";
            case "COMPLAINT_TRANSFER" -> "投诉与人工转接";
            default -> "规则说明";
        };
    }

    private record IntentDecision(String code, double ruleConfidence, double contextConfidence) {
    }

    private record ConversationContext(boolean followUp, String lastIntentCode, String inheritedIntentCode,
                                       String summary, List<ChatMessage> recentMessages) {
        Map<String, Object> toResponse(IntentRecord intent) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("followUp", followUp);
            map.put("lastIntent", lastIntentCode);
            map.put("inheritedIntent", inheritedIntentCode);
            map.put("resolvedIntent", intent.getIntentCode());
            map.put("method", intent.getMethod());
            map.put("summary", summary);
            map.put("recentMessages", recentPromptLines());
            return map;
        }

        List<String> recentPromptLines() {
            if (recentMessages == null || recentMessages.isEmpty()) {
                return List.of();
            }
            return recentMessages.stream()
                    .skip(Math.max(0, recentMessages.size() - 6))
                    .map(message -> ("USER".equals(message.getRole()) ? "用户：" : "客服：") + trimStatic(message.getContent(), 120))
                    .toList();
        }

        private static String trimStatic(String text, int length) {
            if (text == null || text.length() <= length) {
                return text;
            }
            return text.substring(0, length);
        }
    }
}
