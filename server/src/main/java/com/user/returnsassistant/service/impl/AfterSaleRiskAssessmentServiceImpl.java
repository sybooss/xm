package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.AfterSaleRiskAssessmentMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.EvidenceAuditMapper;
import com.user.returnsassistant.mapper.ServiceReviewMapper;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessment;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentRequest;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentSearch;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.EvidenceAudit;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProductIssueAlert;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleRiskAssessmentService;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.ProductIssueInsightService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AfterSaleRiskAssessmentServiceImpl implements AfterSaleRiskAssessmentService {
    @Autowired
    private AfterSaleRiskAssessmentMapper riskAssessmentMapper;
    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private EvidenceAuditMapper evidenceAuditMapper;
    @Autowired
    private ServiceReviewMapper reviewMapper;
    @Autowired
    private AiService aiService;
    @Autowired
    private ProductIssueInsightService productIssueInsightService;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    @Override
    public AfterSaleRiskAssessment assess(Long applicationId, AfterSaleRiskAssessmentRequest request, UserAccount admin) {
        AfterSaleApplication application = requireApplication(applicationId);
        DemoOrder order = orderMapper.getById(application.getOrderId());
        RiskDecision decision = decide(application, order);
        AfterSaleRiskAssessment existing = riskAssessmentMapper.getByApplicationId(applicationId);

        AfterSaleRiskAssessment assessment = existing == null ? new AfterSaleRiskAssessment() : existing;
        if (existing == null) {
            assessment.setAssessmentNo(NoUtils.riskAssessmentNo());
            assessment.setApplicationId(applicationId);
        }
        assessment.setRiskLevel(decision.riskLevel());
        assessment.setRiskScore(decision.riskScore());
        assessment.setRiskTags(String.join("，", decision.tags()));
        assessment.setRiskReasons(String.join("；", decision.reasons()));
        assessment.setSuggestedAction(buildSuggestedAction(decision));
        assessment.setRuleDetailJson(toJson(decision.detail()));
        assessment.setAiSummary(buildSummary(assessment, application, request));
        if (existing == null) {
            riskAssessmentMapper.insert(assessment);
        } else {
            riskAssessmentMapper.update(assessment);
        }
        applicationMapper.updateRiskLevel(applicationId, decision.riskLevel());
        writeLog(application, admin, assessment);
        return hydrate(riskAssessmentMapper.getByApplicationId(applicationId));
    }

    @Override
    public AfterSaleRiskAssessment getByApplicationId(Long applicationId, UserAccount admin) {
        requireApplication(applicationId);
        return hydrate(riskAssessmentMapper.getByApplicationId(applicationId));
    }

    @Override
    public PageResult<AfterSaleRiskAssessment> page(AfterSaleRiskAssessmentSearch search) {
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<AfterSaleRiskAssessment> page = (Page<AfterSaleRiskAssessment>) riskAssessmentMapper.page(search);
        return new PageResult<>(page.getTotal(), page.getResult().stream().map(this::hydrate).toList());
    }

    private RiskDecision decide(AfterSaleApplication application, DemoOrder order) {
        int score = 0;
        Set<String> tags = new LinkedHashSet<>();
        List<String> reasons = new ArrayList<>();
        Map<String, Object> detail = new LinkedHashMap<>();

        BigDecimal refundAmount = application.getRefundAmount();
        if (refundAmount != null && refundAmount.compareTo(new BigDecimal("500.00")) >= 0) {
            score += 15;
            tags.add("高金额");
            reasons.add("申请金额达到或超过 500 元，需要更完整的证据链");
        } else if (refundAmount != null && refundAmount.compareTo(new BigDecimal("300.00")) >= 0) {
            score += 10;
            tags.add("中高金额");
            reasons.add("申请金额达到或超过 300 元，建议管理员复核");
        }
        detail.put("refundAmount", refundAmount);

        long recentAfterSales = applicationMapper.countByUserSince(application.getUserId(), 30);
        detail.put("recentAfterSales30d", recentAfterSales);
        if (recentAfterSales > 3) {
            score += 20;
            tags.add("高频售后用户");
            reasons.add("该用户 30 天内售后次数较多");
        } else if (recentAfterSales >= 2) {
            score += 10;
            tags.add("近期重复售后");
            reasons.add("该用户近期有多次售后记录");
        }

        long complaints = applicationMapper.countByUserServiceType(application.getUserId(), "COMPLAINT");
        detail.put("complaintCount", complaints);
        if ("COMPLAINT".equals(application.getServiceType()) || complaints > 0) {
            score += 20;
            tags.add("投诉风险");
            reasons.add("当前或历史存在投诉类售后，需要人工优先跟进");
        }

        long lowRatings = reviewMapper.countLowRatingByUserIdSince(application.getUserId(), 3, 180);
        detail.put("lowRatings180d", lowRatings);
        if (lowRatings > 1) {
            score += 15;
            tags.add("低满意度历史");
            reasons.add("用户历史低分评价较多，建议客服回复更明确");
        }

        if ("NEED_MORE_EVIDENCE".equals(application.getStatus())) {
            score += 15;
            tags.add("证据不足");
            reasons.add("售后单当前处于待补材料状态");
        }

        List<EvidenceAudit> latestAudits = evidenceAuditMapper.listLatestByApplicationId(application.getId());
        detail.put("evidenceAuditCount", latestAudits.size());
        boolean hasNeedMore = latestAudits.stream().anyMatch(audit -> "NEED_MORE".equals(audit.getAuditStatus()) || "INSUFFICIENT".equals(audit.getSufficiencyLevel()));
        boolean hasRisky = latestAudits.stream().anyMatch(audit -> "RISKY".equals(audit.getAuditStatus()));
        boolean hasAiRisk = latestAudits.stream().anyMatch(audit -> "HIGH".equals(audit.getAiGeneratedRisk()));
        boolean hasTamperRisk = latestAudits.stream().anyMatch(audit -> "HIGH".equals(audit.getTamperRisk()));
        if (hasNeedMore) {
            score += 15;
            tags.add("证据不足");
            reasons.add("最新凭证审核提示材料不足或需补证");
        }
        if (hasRisky) {
            score += 25;
            tags.add("凭证高风险");
            reasons.add("最新凭证审核存在较高真实性风险");
        }
        if (hasAiRisk) {
            score += 20;
            tags.add("疑似 AI 凭证");
            reasons.add("凭证审核提示 AI 生成风险高");
        }
        if (hasTamperRisk) {
            score += 15;
            tags.add("疑似篡改");
            reasons.add("凭证审核提示篡改风险高");
        }

        Long remainingHours = null;
        if (application.getSlaDeadline() != null) {
            remainingHours = Duration.between(LocalDateTime.now(), application.getSlaDeadline()).toHours();
            if (remainingHours < 0) {
                score += 20;
                tags.add("SLA 已超时");
                reasons.add("当前售后已超过 SLA 截止时间");
            } else if (remainingHours <= 24) {
                score += 10;
                tags.add("SLA 临近");
                reasons.add("距离 SLA 截止不足 24 小时");
            }
        }
        detail.put("remainingHours", remainingHours);

        if (order != null && hasText(order.getProductName())) {
            long productRecentAfterSales = orderMapper.countAfterSalesByProductSince(order.getProductName(), 7);
            detail.put("productRecentAfterSales7d", productRecentAfterSales);
            if (productRecentAfterSales > 3) {
                score += 15;
                tags.add("商品集中问题");
                reasons.add("同商品 7 天内售后集中出现，建议关注批次或质量问题");
            }
            List<ProductIssueAlert> productAlerts = productIssueInsightService.listOpenByProduct(order.getProductName(), 7);
            detail.put("productIssueAlertCount", productAlerts.size());
            if (!productAlerts.isEmpty()) {
                ProductIssueAlert topAlert = productAlerts.get(0);
                int alertScore = "HIGH".equals(topAlert.getAlertLevel()) ? 20 : 12;
                score += alertScore;
                tags.add("商品质量预警");
                reasons.add("该商品存在开放质量预警：" + topAlert.getIssueKeyword() + "，等级 " + topAlert.getAlertLevel());
                detail.put("topProductIssueAlert", Map.of(
                        "alertNo", topAlert.getAlertNo(),
                        "issueKeyword", topAlert.getIssueKeyword(),
                        "alertLevel", topAlert.getAlertLevel(),
                        "trendScore", topAlert.getTrendScore()
                ));
            }
        }

        int cappedScore = Math.min(100, score);
        String riskLevel = cappedScore >= 60 ? "HIGH" : (cappedScore >= 30 ? "MEDIUM" : "LOW");
        if (tags.isEmpty()) {
            tags.add("标准风险");
            reasons.add("未发现明显高风险信号，可按标准售后流程处理");
        }
        detail.put("score", cappedScore);
        detail.put("riskLevel", riskLevel);
        detail.put("ruleVersion", "after-sale-risk-v1");
        return new RiskDecision(riskLevel, cappedScore, new ArrayList<>(tags), reasons, detail);
    }

    private String buildSuggestedAction(RiskDecision decision) {
        if ("HIGH".equals(decision.riskLevel())) {
            return "建议资深客服人工复核，先核对凭证原件、订单状态和用户历史，不直接退款或驳回。";
        }
        if ("MEDIUM".equals(decision.riskLevel())) {
            return "建议优先核对凭证和处理记录，必要时要求顾客补充材料后再审核。";
        }
        return "按标准售后规则审核即可，继续记录凭证和处理日志。";
    }

    private String buildSummary(AfterSaleRiskAssessment assessment, AfterSaleApplication application, AfterSaleRiskAssessmentRequest request) {
        if (request == null || !Boolean.FALSE.equals(request.getUseAi())) {
            AiService.AiResult result = aiService.generate(buildPrompt(assessment, application));
            assessment.setAiStatus(result.status());
            assessment.setAiErrorMessage(result.errorMessage());
            if (result.used() && hasText(result.reply())) {
                return trim(result.reply(), 1000);
            }
        } else {
            assessment.setAiStatus("SKIPPED");
            assessment.setAiErrorMessage("本轮使用本地售后风险规则");
        }
        return localSummary(assessment);
    }

    private String buildPrompt(AfterSaleRiskAssessment assessment, AfterSaleApplication application) {
        return "你是电商售后风控辅助助手。请用 120 字以内中文说明该售后单风险，不要直接决定退款、驳回或处罚用户。\n"
                + "售后单：" + application.getApplicationNo() + "\n"
                + "状态：" + application.getStatus() + "\n"
                + "类型：" + application.getServiceType() + "\n"
                + "原因：" + application.getReasonText() + "\n"
                + "风险等级：" + assessment.getRiskLevel() + "\n"
                + "风险分：" + assessment.getRiskScore() + "\n"
                + "标签：" + assessment.getRiskTags() + "\n"
                + "原因：" + assessment.getRiskReasons() + "\n"
                + "建议：" + assessment.getSuggestedAction();
    }

    private String localSummary(AfterSaleRiskAssessment assessment) {
        return "系统按本地规则评估为 " + assessment.getRiskLevel()
                + "，风险分 " + assessment.getRiskScore()
                + "。主要信号：" + assessment.getRiskTags()
                + "。最终处理仍需管理员结合订单、凭证和规则复核。";
    }

    private void writeLog(AfterSaleApplication application, UserAccount operator, AfterSaleRiskAssessment assessment) {
        AfterSaleProcessLog log = new AfterSaleProcessLog();
        log.setApplicationId(application.getId());
        log.setOperatorId(operator.getId());
        log.setOperatorName(operator.getDisplayName());
        log.setOperatorRole(operator.getRole());
        log.setAction("RISK_ASSESSMENT");
        log.setFromStatus(application.getStatus());
        log.setToStatus(application.getStatus());
        log.setRemark("售后风险评估 " + assessment.getAssessmentNo()
                + "：" + assessment.getRiskLevel()
                + "，分数 " + assessment.getRiskScore()
                + "，标签 " + trim(assessment.getRiskTags(), 160));
        processLogMapper.insert(log);
    }

    private AfterSaleApplication requireApplication(Long applicationId) {
        if (applicationId == null) {
            throw new BusinessException("售后申请 ID 不能为空");
        }
        AfterSaleApplication application = applicationMapper.getById(applicationId);
        if (application == null) {
            throw new BusinessException("售后申请不存在");
        }
        return application;
    }

    private AfterSaleRiskAssessment hydrate(AfterSaleRiskAssessment assessment) {
        if (assessment == null) {
            return null;
        }
        assessment.setRiskTagList(split(assessment.getRiskTags()));
        assessment.setRiskReasonList(split(assessment.getRiskReasons()));
        return assessment;
    }

    private List<String> split(String text) {
        if (!hasText(text)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String item : text.split("[,，；;\\n]")) {
            String clean = item.trim();
            if (hasText(clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String trim(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private record RiskDecision(
            String riskLevel,
            Integer riskScore,
            List<String> tags,
            List<String> reasons,
            Map<String, Object> detail
    ) {
    }
}
