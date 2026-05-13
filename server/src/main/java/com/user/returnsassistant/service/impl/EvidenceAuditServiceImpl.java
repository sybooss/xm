package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.AfterSaleEvidenceMapper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.EvidenceAuditMapper;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleEvidence;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.EvidenceAudit;
import com.user.returnsassistant.pojo.EvidenceAuditRequest;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.EvidenceAuditService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvidenceAuditServiceImpl implements EvidenceAuditService {
    @Autowired
    private EvidenceAuditMapper evidenceAuditMapper;
    @Autowired
    private AfterSaleEvidenceMapper evidenceMapper;
    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;
    @Autowired
    private AuthService authService;
    @Autowired
    private AiService aiService;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    @Override
    public EvidenceAudit audit(Long evidenceId, EvidenceAuditRequest request, UserAccount user) {
        AfterSaleEvidence evidence = requireEvidence(evidenceId);
        AfterSaleApplication application = requireApplication(evidence.getApplicationId());
        ensureApplicationAccess(application, user);

        RuleDecision decision = decide(application, evidence);
        EvidenceAudit audit = new EvidenceAudit();
        audit.setAuditNo(NoUtils.evidenceAuditNo());
        audit.setApplicationId(application.getId());
        audit.setEvidenceId(evidence.getId());
        audit.setAuditStatus(decision.auditStatus());
        audit.setSufficiencyLevel(decision.sufficiencyLevel());
        audit.setAuthenticityRisk(decision.authenticityRisk());
        audit.setAiGeneratedRisk(decision.aiGeneratedRisk());
        audit.setTamperRisk(decision.tamperRisk());
        audit.setMetadataSignal(joinOrDefault(decision.metadataSignals(), "未发现明确元数据异常信号，第一版未读取真实 EXIF/C2PA。"));
        audit.setVisualSignal(joinOrDefault(decision.visualSignals(), "凭证描述与售后问题未发现明显冲突，仍需人工结合原始文件判断。"));
        audit.setWatermarkSignal(joinOrDefault(decision.watermarkSignals(), "未发现明确水印或生成平台来源信号。"));
        audit.setRequiredEvidence(joinOrDefault(decision.requiredEvidence(), "当前凭证基本充分，管理员仍需结合订单规则复核。"));
        audit.setAuditDetailJson(toJson(decision.detail()));
        audit.setAiSummary(buildSummary(audit, application, evidence, request));
        evidenceAuditMapper.insert(audit);
        writeLog(application, user, audit);
        return hydrate(evidenceAuditMapper.getById(audit.getId()));
    }

    @Override
    public List<EvidenceAudit> listByEvidenceId(Long evidenceId, UserAccount user) {
        AfterSaleEvidence evidence = requireEvidence(evidenceId);
        AfterSaleApplication application = requireApplication(evidence.getApplicationId());
        ensureApplicationAccess(application, user);
        return evidenceAuditMapper.listByEvidenceId(evidenceId).stream().map(this::hydrate).toList();
    }

    @Override
    public List<EvidenceAudit> listByApplicationId(Long applicationId, UserAccount user) {
        AfterSaleApplication application = requireApplication(applicationId);
        ensureApplicationAccess(application, user);
        return evidenceAuditMapper.listByApplicationId(applicationId).stream().map(this::hydrate).toList();
    }

    @Override
    public void attachLatestAudits(Long applicationId, List<AfterSaleEvidence> evidences) {
        if (applicationId == null || evidences == null || evidences.isEmpty()) {
            return;
        }
        Map<Long, EvidenceAudit> latestMap = evidenceAuditMapper.listLatestByApplicationId(applicationId).stream()
                .map(this::hydrate)
                .collect(Collectors.toMap(EvidenceAudit::getEvidenceId, Function.identity(), (left, right) -> left));
        for (AfterSaleEvidence evidence : evidences) {
            evidence.setLatestAudit(latestMap.get(evidence.getId()));
        }
    }

    private RuleDecision decide(AfterSaleApplication application, AfterSaleEvidence evidence) {
        String text = normalize((evidence.getContent() == null ? "" : evidence.getContent())
                + " " + (evidence.getFileUrl() == null ? "" : evidence.getFileUrl())
                + " " + (application.getReasonText() == null ? "" : application.getReasonText()));
        Set<String> required = new LinkedHashSet<>();
        Set<String> metadataSignals = new LinkedHashSet<>();
        Set<String> visualSignals = new LinkedHashSet<>();
        Set<String> watermarkSignals = new LinkedHashSet<>();
        Map<String, Object> detail = new LinkedHashMap<>();

        String evidenceType = evidence.getEvidenceType() == null ? "TEXT" : evidence.getEvidenceType();
        int contentLength = evidence.getContent() == null ? 0 : evidence.getContent().trim().length();
        boolean hasFileUrl = hasText(evidence.getFileUrl());
        boolean issueMatched = issueMatched(application, text);
        boolean logisticsLike = "LOGISTICS_NO".equals(evidenceType) || containsAny(text, "sf", "yt", "sto", "jd", "ems", "快递", "物流", "单号", "签收", "拒收");
        boolean imageOrVideo = "IMAGE".equals(evidenceType) || "VIDEO".equals(evidenceType);

        if (contentLength < 12) {
            required.add("补充更完整的问题说明，说明故障现象、发生时间和期望处理方式");
        }
        if (imageOrVideo && !hasFileUrl && !containsAny(text, "照片", "图片", "视频", "截图", "外观", "故障")) {
            required.add("上传原始图片或视频链接，保留拍摄时间和商品外观信息");
        }
        if (("EXCHANGE".equals(application.getServiceType()) || containsAny(text, "故障", "坏", "断连", "没声音", "无法充电", "失灵"))
                && !containsAny(text, "视频", "故障", "外观", "序列号", "充电", "声音", "断连")) {
            required.add("补充故障视频、商品外观照片和序列号照片");
        }
        if (("REFUND".equals(application.getServiceType()) || containsAny(text, "没收到", "少件", "拒收", "破损"))
                && !logisticsLike) {
            required.add("补充物流轨迹截图、签收或拒收说明");
        }
        if ("RETURN".equals(application.getServiceType()) && !containsAny(text, "包装", "配件", "吊牌", "外观", "不影响二次销售")) {
            required.add("补充包装完整性、配件齐全和商品外观照片");
        }

        if (!issueMatched) {
            visualSignals.add("凭证描述与售后原因匹配度较弱，建议补充能直接证明问题的原始材料。");
        }
        if (containsAny(text, "ai生成", "ai 生成", "生成图", "aigc", "midjourney", "stable diffusion", "stablediffusion", "dall-e", "dalle", "dream", "synthetic")) {
            watermarkSignals.add("凭证说明或链接包含生成式图片平台/AI 生成相关信号。");
        }
        if (containsAny(text, "无原图", "网图", "示意图", "参考图", "合成", "渲染", "非实拍")) {
            visualSignals.add("凭证可能不是商品实拍或原始材料，需要补充实拍原图。");
        }
        if (containsAny(text, "photoshop", "p图", "修图", "裁剪", "二次处理", "编辑软件", "美化", "拼接", "马赛克") || hasStandalonePsSignal(text)) {
            metadataSignals.add("凭证出现编辑、裁剪或二次处理描述，存在篡改风险信号。");
        }
        if (containsAny(text, "c2pa", "exif", "水印", "hidden watermark", "隐形水印", "来源凭证")) {
            metadataSignals.add("凭证提到水印、EXIF、C2PA 或来源信息，需要人工查看原始文件确认。");
        }

        int aiRiskScore = watermarkSignals.size() * 2 + (containsAny(text, "无原图", "网图", "示意图", "非实拍") ? 1 : 0);
        int tamperRiskScore = metadataSignals.size() * 2 + (containsAny(text, "马赛克", "拼接", "裁剪") ? 1 : 0);
        int authenticityScore = aiRiskScore + tamperRiskScore + (issueMatched ? 0 : 1);
        BigDecimal amount = application.getRefundAmount();
        if (amount != null && amount.compareTo(new BigDecimal("300.00")) >= 0) {
            authenticityScore++;
            detail.put("highAmount", true);
        }

        String sufficiencyLevel = required.isEmpty() ? "SUFFICIENT" : (required.size() <= 2 ? "PARTIAL" : "INSUFFICIENT");
        String aiGeneratedRisk = toRisk(aiRiskScore);
        String tamperRisk = toRisk(tamperRiskScore);
        String authenticityRisk = toRisk(authenticityScore);
        String auditStatus = decideAuditStatus(sufficiencyLevel, authenticityRisk, aiGeneratedRisk, tamperRisk, application);

        detail.put("evidenceType", evidenceType);
        detail.put("contentLength", contentLength);
        detail.put("hasFileUrl", hasFileUrl);
        detail.put("issueMatched", issueMatched);
        detail.put("logisticsLike", logisticsLike);
        detail.put("serviceType", application.getServiceType());
        detail.put("riskScore", authenticityScore);
        detail.put("ruleVersion", "local-evidence-audit-v1");

        return new RuleDecision(auditStatus, sufficiencyLevel, authenticityRisk, aiGeneratedRisk, tamperRisk,
                new ArrayList<>(metadataSignals), new ArrayList<>(visualSignals), new ArrayList<>(watermarkSignals),
                new ArrayList<>(required), detail);
    }

    private boolean issueMatched(AfterSaleApplication application, String text) {
        String reason = normalize(application.getReasonText());
        if (!hasText(reason)) {
            return true;
        }
        if (containsAny(text, application.getServiceType(), application.getReasonCode())) {
            return true;
        }
        if (containsAny(reason, "故障", "质量", "坏", "断连", "没声音", "充电", "失灵")) {
            return containsAny(text, "故障", "质量", "坏", "断连", "没声音", "充电", "失灵", "视频", "外观");
        }
        if (containsAny(reason, "物流", "没收到", "少件", "拒收", "破损")) {
            return containsAny(text, "物流", "快递", "单号", "签收", "拒收", "破损", "轨迹");
        }
        if (containsAny(reason, "不想要", "不满意", "七天", "退货")) {
            return containsAny(text, "包装", "外观", "配件", "不影响", "照片", "退货");
        }
        return true;
    }

    private String decideAuditStatus(String sufficiencyLevel, String authenticityRisk, String aiGeneratedRisk, String tamperRisk, AfterSaleApplication application) {
        if ("HIGH".equals(authenticityRisk) || "HIGH".equals(aiGeneratedRisk) || "HIGH".equals(tamperRisk)) {
            return "RISKY";
        }
        if ("COMPLAINT".equals(application.getServiceType()) || "MEDIUM".equals(authenticityRisk)) {
            return "MANUAL_REVIEW";
        }
        if (!"SUFFICIENT".equals(sufficiencyLevel)) {
            return "NEED_MORE";
        }
        return "PASS";
    }

    private String toRisk(int score) {
        if (score >= 4) {
            return "HIGH";
        }
        if (score >= 2) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String buildSummary(EvidenceAudit audit, AfterSaleApplication application, AfterSaleEvidence evidence, EvidenceAuditRequest request) {
        if (request != null && Boolean.TRUE.equals(request.getUseAi())) {
            AiService.AiResult result = aiService.generate(buildPrompt(audit, application, evidence));
            audit.setAiStatus(result.status());
            audit.setAiErrorMessage(result.errorMessage());
            if (result.used() && hasText(result.reply())) {
                return trim(result.reply(), 1000);
            }
        } else {
            audit.setAiStatus("SKIPPED");
            audit.setAiErrorMessage("本轮使用本地凭证审核规则");
        }
        return localSummary(audit);
    }

    private String buildPrompt(EvidenceAudit audit, AfterSaleApplication application, AfterSaleEvidence evidence) {
        return "你是电商售后凭证审核助手。请基于规则审核结果，用 120 字以内中文说明凭证是否充分、有哪些风险信号、建议补什么。"
                + "不要说绝对真假，不要承诺退款或驳回。\n"
                + "售后类型：" + application.getServiceType() + "\n"
                + "售后原因：" + application.getReasonText() + "\n"
                + "凭证类型：" + evidence.getEvidenceType() + "\n"
                + "凭证内容：" + evidence.getContent() + "\n"
                + "审核状态：" + audit.getAuditStatus() + "\n"
                + "充分性：" + audit.getSufficiencyLevel() + "\n"
                + "真实性风险：" + audit.getAuthenticityRisk() + "\n"
                + "AI生成风险：" + audit.getAiGeneratedRisk() + "\n"
                + "篡改风险：" + audit.getTamperRisk() + "\n"
                + "补证建议：" + audit.getRequiredEvidence();
    }

    private String localSummary(EvidenceAudit audit) {
        if ("PASS".equals(audit.getAuditStatus())) {
            return "当前凭证较充分，暂未发现明显 AI 生成或篡改风险信号。最终仍需客服结合订单规则审核。";
        }
        if ("NEED_MORE".equals(audit.getAuditStatus())) {
            return "当前凭证还不足以支撑直接处理，建议补充：" + audit.getRequiredEvidence();
        }
        if ("RISKY".equals(audit.getAuditStatus())) {
            return "凭证存在来源不清、疑似生成或二次处理风险信号，建议补充原始实拍材料并人工复核。";
        }
        return "该凭证建议进入人工复核，客服需结合订单、商品、原始文件和处理记录综合判断。";
    }

    private void writeLog(AfterSaleApplication application, UserAccount operator, EvidenceAudit audit) {
        AfterSaleProcessLog log = new AfterSaleProcessLog();
        log.setApplicationId(application.getId());
        log.setOperatorId(operator == null ? null : operator.getId());
        log.setOperatorName(operator == null ? "系统" : operator.getDisplayName());
        log.setOperatorRole(operator == null ? "SYSTEM" : operator.getRole());
        log.setAction("EVIDENCE_AUDIT");
        log.setFromStatus(application.getStatus());
        log.setToStatus(application.getStatus());
        log.setRemark("凭证审核 " + audit.getAuditNo()
                + "：" + audit.getAuditStatus()
                + "，充分性 " + audit.getSufficiencyLevel()
                + "，真实性风险 " + audit.getAuthenticityRisk()
                + "，建议：" + trim(audit.getRequiredEvidence(), 220));
        processLogMapper.insert(log);
    }

    private AfterSaleEvidence requireEvidence(Long evidenceId) {
        if (evidenceId == null) {
            throw new BusinessException("凭证 ID 不能为空");
        }
        AfterSaleEvidence evidence = evidenceMapper.getById(evidenceId);
        if (evidence == null) {
            throw new BusinessException("凭证不存在");
        }
        return evidence;
    }

    private AfterSaleApplication requireApplication(Long applicationId) {
        AfterSaleApplication application = applicationMapper.getById(applicationId);
        if (application == null) {
            throw new BusinessException("售后申请不存在");
        }
        return application;
    }

    private void ensureApplicationAccess(AfterSaleApplication application, UserAccount user) {
        if (user == null) {
            throw new BusinessException("请先登录");
        }
        authService.ensureSelfOrAdmin(user, application.getUserId(), "只能查看自己的售后凭证审核");
    }

    private EvidenceAudit hydrate(EvidenceAudit audit) {
        if (audit == null) {
            return null;
        }
        audit.setRequiredEvidenceList(splitEvidence(audit.getRequiredEvidence()));
        return audit;
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

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String joinOrDefault(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return String.join("；", values);
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!hasText(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStandalonePsSignal(String text) {
        if (!hasText(text)) {
            return false;
        }
        return text.matches(".*(^|[^a-z0-9])ps([^a-z0-9]|$).*");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private record RuleDecision(
            String auditStatus,
            String sufficiencyLevel,
            String authenticityRisk,
            String aiGeneratedRisk,
            String tamperRisk,
            List<String> metadataSignals,
            List<String> visualSignals,
            List<String> watermarkSignals,
            List<String> requiredEvidence,
            Map<String, Object> detail
    ) {
    }
}
