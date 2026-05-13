package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleDiagnosisMapper;
import com.user.returnsassistant.mapper.ChatSessionMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.pojo.AfterSaleDiagnosis;
import com.user.returnsassistant.pojo.AfterSaleDiagnosisRequest;
import com.user.returnsassistant.pojo.AfterSaleSolutionOption;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.ProductInsight;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleDiagnosisService;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.ProductInsightService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AfterSaleDiagnosisServiceImpl implements AfterSaleDiagnosisService {
    private static final TypeReference<List<AfterSaleSolutionOption>> OPTION_LIST_TYPE = new TypeReference<>() {
    };

    @Autowired
    private AfterSaleDiagnosisMapper diagnosisMapper;
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ProductInsightService productInsightService;
    @Autowired
    private AiService aiService;
    @Autowired
    private AuthService authService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AfterSaleDiagnosis diagnose(AfterSaleDiagnosisRequest request, UserAccount user) {
        if (request == null) {
            throw new BusinessException("请填写诊断信息");
        }
        String issueText = cleanRequired(request.getIssueText(), "请填写问题说明后再诊断");
        DemoOrder order = findOrder(request);
        ensureOrderAccess(order, user);
        ensureSessionAccess(request.getSessionId(), order.getId(), user);

        ProductInsight productInsight = productInsightService.build(order, issueText, null, false);
        String suggestedServiceType = suggestServiceType(issueText, request.getServiceType(), productInsight);
        String decisionLevel = decideLevel(order, issueText, suggestedServiceType, request.getRefundAmount());
        List<String> requiredEvidence = requiredEvidence(order, issueText, suggestedServiceType, decisionLevel, request.getRefundAmount());
        List<AfterSaleSolutionOption> options = solutionOptions(suggestedServiceType, decisionLevel, productInsight);

        AfterSaleDiagnosis diagnosis = new AfterSaleDiagnosis();
        diagnosis.setDiagnosisNo(NoUtils.diagnosisNo());
        diagnosis.setSessionId(request.getSessionId());
        diagnosis.setOrderId(order.getId());
        diagnosis.setUserId(order.getUserId());
        diagnosis.setIssueText(issueText);
        diagnosis.setSuggestedServiceType(suggestedServiceType);
        diagnosis.setDecisionLevel(decisionLevel);
        diagnosis.setReasonSummary(buildReasonSummary(order, issueText, suggestedServiceType, decisionLevel, productInsight));
        diagnosis.setRequiredEvidence(String.join("；", requiredEvidence));
        diagnosis.setSolutionOptionsJson(toJson(options));
        diagnosis.setAiSummary(buildAiSummary(diagnosis, productInsight, request.getUseAi()));
        diagnosisMapper.insert(diagnosis);
        return hydrate(diagnosisMapper.getById(diagnosis.getId()));
    }

    @Override
    public AfterSaleDiagnosis getById(Long id, UserAccount user) {
        AfterSaleDiagnosis diagnosis = diagnosisMapper.getById(id);
        if (diagnosis == null) {
            throw new BusinessException("诊断结果不存在");
        }
        ensureDiagnosisAccess(diagnosis, user);
        return hydrate(diagnosis);
    }

    @Override
    public AfterSaleDiagnosis getLatestBySessionId(Long sessionId, UserAccount user) {
        if (sessionId == null) {
            throw new BusinessException("会话 ID 不能为空");
        }
        ChatSession session = sessionMapper.getById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        authService.ensureSelfOrAdmin(user, session.getUserId(), "只能查看自己的会话诊断");
        AfterSaleDiagnosis diagnosis = diagnosisMapper.getLatestBySessionId(sessionId);
        if (diagnosis == null) {
            throw new BusinessException("该会话暂无售后诊断");
        }
        ensureDiagnosisAccess(diagnosis, user);
        return hydrate(diagnosis);
    }

    @Override
    public AfterSaleDiagnosis getInternal(Long id) {
        if (id == null) {
            return null;
        }
        return hydrate(diagnosisMapper.getById(id));
    }

    @Override
    public AfterSaleDiagnosis getOwnedForBinding(Long id, Long orderId, UserAccount user) {
        if (id == null) {
            return null;
        }
        AfterSaleDiagnosis diagnosis = diagnosisMapper.getById(id);
        if (diagnosis == null) {
            throw new BusinessException("诊断结果不存在");
        }
        ensureDiagnosisAccess(diagnosis, user);
        if (!Objects.equals(diagnosis.getOrderId(), orderId)) {
            throw new BusinessException("诊断结果与当前订单不匹配");
        }
        if (diagnosis.getApplicationId() != null) {
            throw new BusinessException("该诊断结果已绑定售后申请");
        }
        return hydrate(diagnosis);
    }

    @Override
    public void bindApplication(Long diagnosisId, Long applicationId) {
        if (diagnosisId != null && applicationId != null) {
            diagnosisMapper.bindApplication(diagnosisId, applicationId);
        }
    }

    private DemoOrder findOrder(AfterSaleDiagnosisRequest request) {
        DemoOrder order = null;
        if (request.getOrderId() != null) {
            order = orderMapper.getById(request.getOrderId());
        } else if (hasText(request.getOrderNo())) {
            order = orderMapper.getByOrderNo(request.getOrderNo().trim());
        }
        if (order == null) {
            throw new BusinessException("订单不存在，无法诊断");
        }
        return order;
    }

    private void ensureOrderAccess(DemoOrder order, UserAccount user) {
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        if (!authService.isAdmin(user) && !Objects.equals(order.getUserId(), user.getId())) {
            throw new BusinessException("只能诊断自己的订单");
        }
    }

    private void ensureSessionAccess(Long sessionId, Long orderId, UserAccount user) {
        if (sessionId == null) {
            return;
        }
        ChatSession session = sessionMapper.getById(sessionId);
        if (session == null) {
            throw new BusinessException("来源会话不存在");
        }
        authService.ensureSelfOrAdmin(user, session.getUserId(), "只能把诊断绑定到自己的会话");
        if (session.getOrderId() != null && !Objects.equals(session.getOrderId(), orderId)) {
            throw new BusinessException("来源会话绑定的订单与诊断订单不一致");
        }
    }

    private void ensureDiagnosisAccess(AfterSaleDiagnosis diagnosis, UserAccount user) {
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        if (!authService.isAdmin(user) && !Objects.equals(diagnosis.getUserId(), user.getId())) {
            throw new BusinessException("只能查看自己的诊断结果");
        }
    }

    private String suggestServiceType(String issueText, String requestedType, ProductInsight insight) {
        String text = normalize(issueText);
        if (containsAny(text, "投诉", "人工", "商家不处理", "没人处理", "生气", "差评")) {
            return "COMPLAINT";
        }
        if (containsAny(text, "仅退款", "没收到", "少件", "漏发", "拒收")) {
            return "REFUND";
        }
        if (containsAny(text, "断连", "连不上", "单耳", "没声音", "无法充电", "充不进", "按键失灵", "连击", "黑屏", "花屏", "质量问题", "故障")) {
            return "EXCHANGE";
        }
        if (containsAny(text, "维修", "修一下", "检测")) {
            return "REPAIR";
        }
        if (containsAny(text, "退货", "想退", "要退", "不想要", "七天无理由", "体验不好", "不满意")) {
            return "RETURN";
        }
        if (hasText(requestedType) && List.of("RETURN", "EXCHANGE", "REFUND", "COMPLAINT").contains(requestedType.trim().toUpperCase(Locale.ROOT))) {
            return requestedType.trim().toUpperCase(Locale.ROOT);
        }
        if (insight != null && insight.getMatchedConcerns() != null && !insight.getMatchedConcerns().contains("GENERAL_PRODUCT_USAGE")) {
            return "EXCHANGE";
        }
        return "RETURN";
    }

    private String decideLevel(DemoOrder order, String issueText, String suggestedServiceType, BigDecimal refundAmount) {
        if (!"PAID".equals(order.getPayStatus()) || "PENDING_PAY".equals(order.getOrderStatus()) || "CLOSED".equals(order.getOrderStatus())) {
            return "REJECT_SUGGESTED";
        }
        if (!"NONE".equals(order.getAfterSaleStatus())) {
            return "REJECT_SUGGESTED";
        }
        if ("COMPLAINT".equals(suggestedServiceType)) {
            return "MANUAL_REVIEW";
        }
        BigDecimal amount = refundAmount == null ? order.getOrderAmount() : refundAmount;
        if (amount != null && amount.compareTo(new BigDecimal("500.00")) >= 0) {
            return "MANUAL_REVIEW";
        }
        String text = normalize(issueText);
        if (containsAny(text, "没收到", "拒收", "物流", "丢件", "破损")) {
            return "MANUAL_REVIEW";
        }
        if (containsAny(text, "质量问题", "故障", "断连", "无法充电", "没声音", "失灵", "降噪", "体验不好", "不满意")) {
            return "NEED_EVIDENCE";
        }
        if (order.getSignedAt() != null && ChronoUnit.DAYS.between(order.getSignedAt(), LocalDateTime.now()) > 7 && "RETURN".equals(suggestedServiceType)) {
            return "MANUAL_REVIEW";
        }
        return "ALLOW";
    }

    private List<String> requiredEvidence(DemoOrder order, String issueText, String suggestedServiceType, String decisionLevel, BigDecimal refundAmount) {
        List<String> result = new ArrayList<>();
        String text = normalize(issueText);
        if ("REJECT_SUGGESTED".equals(decisionLevel)) {
            result.add("订单状态截图");
            result.add("补充说明是否已有售后处理");
            return result;
        }
        if (containsAny(text, "物流", "没收到", "拒收", "丢件", "破损")) {
            result.add("物流轨迹截图");
            result.add("签收或拒收说明");
        }
        if (containsAny(text, "故障", "质量问题", "断连", "无法充电", "没声音", "失灵", "连击", "黑屏", "花屏")) {
            result.add("故障现象视频");
            result.add("商品外观照片");
        }
        if (containsAny(text, "降噪", "体验不好", "不满意", "不想要") || "RETURN".equals(suggestedServiceType)) {
            result.add("商品实拍图");
            result.add("包装完整性说明");
        }
        if ("COMPLAINT".equals(suggestedServiceType)) {
            result.add("沟通记录截图");
            result.add("期望处理方式");
        }
        BigDecimal amount = refundAmount == null ? order.getOrderAmount() : refundAmount;
        if (amount != null && amount.compareTo(new BigDecimal("500.00")) >= 0) {
            result.add("商品全套照片");
            result.add("配件清单说明");
        }
        if (result.isEmpty()) {
            result.add("问题说明");
            result.add("商品当前状态照片");
        }
        return result.stream().distinct().toList();
    }

    private List<AfterSaleSolutionOption> solutionOptions(String suggestedServiceType, String decisionLevel, ProductInsight insight) {
        List<AfterSaleSolutionOption> options = new ArrayList<>();
        if (insight != null && insight.getTroubleshootingSteps() != null && !insight.getTroubleshootingSteps().isEmpty()) {
            options.add(new AfterSaleSolutionOption("KEEP", "先按商品排查建议确认问题", String.join("；", insight.getTroubleshootingSteps().stream().limit(3).toList()), "低"));
        } else {
            options.add(new AfterSaleSolutionOption("KEEP", "先补充问题现象并做基础排查", "确认是否为设置、使用场景或配件导致的问题。", "低"));
        }
        switch (suggestedServiceType) {
            case "EXCHANGE" -> options.add(new AfterSaleSolutionOption("EXCHANGE", "疑似质量故障时换货检测", "上传故障视频和商品外观照片后，由客服审核换货路径。", "中"));
            case "REFUND" -> options.add(new AfterSaleSolutionOption("REFUND", "仅退款或物流异常处理", "适用于未收到货、少件、拒收或商家确认无需退回的场景。", "中"));
            case "COMPLAINT" -> options.add(new AfterSaleSolutionOption("COMPLAINT", "转人工复核投诉", "人工客服结合订单、沟通记录和 SLA 优先处理。", "高"));
            case "REPAIR" -> options.add(new AfterSaleSolutionOption("REPAIR", "先维修检测或补充检测材料", "适用于超过常规退换期但仍需排查质量问题的场景。", "中"));
            default -> options.add(new AfterSaleSolutionOption("RETURN", "按规则申请退货退款", "商品完好、配件齐全时可按平台规则提交退货退款。", "需确认商品完好"));
        }
        if ("NEED_EVIDENCE".equals(decisionLevel)) {
            options.add(new AfterSaleSolutionOption("EVIDENCE", "先补齐凭证再审核", "补充图片、视频或物流截图后，审核判断会更稳。", "低"));
        }
        if ("MANUAL_REVIEW".equals(decisionLevel)) {
            options.add(new AfterSaleSolutionOption("MANUAL", "人工客服复核", "高金额、投诉或物流异常建议交由人工确认。", "高"));
        }
        return options;
    }

    private String buildReasonSummary(DemoOrder order, String issueText, String suggestedServiceType, String decisionLevel, ProductInsight insight) {
        String signedText = order.getSignedAt() == null ? "订单尚未记录签收时间" : "订单已签收 " + ChronoUnit.DAYS.between(order.getSignedAt(), LocalDateTime.now()) + " 天";
        String productText = insight == null || !hasText(insight.getLocalSummary()) ? "" : "；" + insight.getLocalSummary();
        return signedText + "，当前订单状态为 " + order.getOrderStatus()
                + "，建议路径为 " + suggestedServiceType
                + "，决策等级为 " + decisionLevel
                + "。用户描述：" + trim(issueText, 120)
                + productText;
    }

    private String buildAiSummary(AfterSaleDiagnosis diagnosis, ProductInsight productInsight, Boolean useAi) {
        if (useAi == null || !useAi) {
            diagnosis.setAiStatus("SKIPPED");
            diagnosis.setAiErrorMessage("本轮使用本地规则生成诊断");
            return localAiSummary(diagnosis);
        }
        AiService.AiResult result = aiService.generate(buildPrompt(diagnosis, productInsight));
        diagnosis.setAiStatus(result.status());
        diagnosis.setAiErrorMessage(result.errorMessage());
        if (result.used() && hasText(result.reply())) {
            return trim(result.reply(), 1000);
        }
        return localAiSummary(diagnosis);
    }

    private String localAiSummary(AfterSaleDiagnosis diagnosis) {
        return "系统建议选择 " + diagnosis.getSuggestedServiceType()
                + "，当前判断为 " + diagnosis.getDecisionLevel()
                + "。请先核对订单状态并补充凭证，最终处理以客服审核结果为准。";
    }

    private String buildPrompt(AfterSaleDiagnosis diagnosis, ProductInsight productInsight) {
        return "你是电商售后前置诊断助手。请根据以下信息输出一段 160 字以内的中文说明，只能给建议，不能承诺退款或驳回。\n"
                + "问题：" + diagnosis.getIssueText() + "\n"
                + "推荐类型：" + diagnosis.getSuggestedServiceType() + "\n"
                + "决策等级：" + diagnosis.getDecisionLevel() + "\n"
                + "原因：" + diagnosis.getReasonSummary() + "\n"
                + "需补凭证：" + diagnosis.getRequiredEvidence() + "\n"
                + "商品洞察：" + (productInsight == null ? "" : productInsight.getLocalSummary());
    }

    private AfterSaleDiagnosis hydrate(AfterSaleDiagnosis diagnosis) {
        if (diagnosis == null) {
            return null;
        }
        diagnosis.setRequiredEvidenceList(splitEvidence(diagnosis.getRequiredEvidence()));
        diagnosis.setSolutionOptions(parseOptions(diagnosis.getSolutionOptionsJson()));
        return diagnosis;
    }

    private List<String> splitEvidence(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : text.split("[；;\\n]")) {
            String clean = item.trim();
            if (hasText(clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    private List<AfterSaleSolutionOption> parseOptions(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, OPTION_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<AfterSaleSolutionOption> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String cleanRequired(String text, String message) {
        if (!hasText(text)) {
            throw new BusinessException(message);
        }
        return text.trim();
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
