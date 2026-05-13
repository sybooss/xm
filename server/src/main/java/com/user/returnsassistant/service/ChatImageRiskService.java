package com.user.returnsassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.mapper.ProcessTraceMapper;
import com.user.returnsassistant.pojo.ChatImageRisk;
import com.user.returnsassistant.pojo.ChatMessage;
import com.user.returnsassistant.pojo.ChatMessageRequest;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.ProcessTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatImageRiskService {
    public static final String TRACE_STEP = "CHAT_IMAGE_RISK_SCAN";

    @Autowired
    private ProcessTraceMapper processTraceMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public ChatImageRisk scan(ChatMessageRequest request, String userContent, DemoOrder order) {
        if (request == null || !hasText(request.getFileUrl())) {
            return null;
        }
        String text = normalize(nullToEmpty(userContent)
                + " " + nullToEmpty(request.getFileUrl())
                + " " + nullToEmpty(request.getOriginalFilename())
                + " " + nullToEmpty(request.getContentType())
                + " " + (order == null ? "" : nullToEmpty(order.getProductName()))
                + " " + (order == null ? "" : nullToEmpty(order.getAfterSaleStatus())));

        Set<String> required = new LinkedHashSet<>();
        Set<String> metadataSignals = new LinkedHashSet<>();
        Set<String> visualSignals = new LinkedHashSet<>();
        Set<String> watermarkSignals = new LinkedHashSet<>();

        int contentLength = userContent == null ? 0 : userContent.trim().length();
        boolean imageType = normalize(request.getContentType()).startsWith("image/");
        boolean issueMatched = issueMatched(text);
        int aiRiskScore = 0;
        int tamperRiskScore = 0;
        int authenticityScore = 0;

        if (!imageType) {
            metadataSignals.add("文件类型不是标准图片 MIME，建议客服确认原始文件格式。");
            authenticityScore++;
        }
        if (request.getFileSize() == null) {
            metadataSignals.add("缺少文件大小信息，建议保留原始文件后再复核。");
            authenticityScore++;
        } else if (request.getFileSize() < 1024) {
            metadataSignals.add("图片文件体积异常偏小，可能不是完整原始照片。");
            tamperRiskScore++;
        }
        if (contentLength < 16) {
            required.add("补充图片说明，说明故障现象、出现时间和期望处理方式");
        }
        if (!containsAny(text, "视频", "序列号", "外观", "包装", "配件", "物流", "单号", "原图", "实拍", "照片")) {
            required.add("补充原始实拍图、故障视频、序列号照片或包装配件照片");
        }

        if (containsAny(text, "ai生成", "ai 生成", "aigc", "生成图", "midjourney", "stable diffusion", "stablediffusion", "dall-e", "dalle", "synthetic")) {
            watermarkSignals.add("聊天图片说明或文件信息包含 AI 生成/AIGC/生成平台相关信号。");
            aiRiskScore += 3;
        }
        if (containsAny(text, "无原图", "网图", "示意图", "参考图", "合成", "渲染", "非实拍")) {
            visualSignals.add("图片来源可能不是商品实拍，建议补充原始拍摄材料。");
            aiRiskScore++;
            authenticityScore++;
        }
        if (containsAny(text, "photoshop", "p图", "修图", "裁剪", "二次处理", "编辑软件", "美化", "拼接", "马赛克") || hasStandalonePsSignal(text)) {
            metadataSignals.add("图片描述出现编辑、裁剪或二次处理信号，需要人工复核。");
            tamperRiskScore += 2;
        }
        if (containsAny(text, "c2pa", "exif", "水印", "hidden watermark", "隐形水印", "来源凭证")) {
            metadataSignals.add("图片提到 EXIF、C2PA、水印或来源凭证，建议客服查看原始文件信息。");
            watermarkSignals.add("存在水印或来源信息复核诉求，不能只看聊天缩略图下结论。");
            authenticityScore++;
        }
        if (!issueMatched) {
            visualSignals.add("图片说明与常见售后故障信号匹配度较弱，建议补充能直接证明问题的材料。");
            authenticityScore++;
        }

        authenticityScore += aiRiskScore + tamperRiskScore;
        String sufficiencyLevel = required.isEmpty() ? "SUFFICIENT" : (required.size() <= 2 ? "PARTIAL" : "INSUFFICIENT");
        String aiGeneratedRisk = toRisk(aiRiskScore);
        String tamperRisk = toRisk(tamperRiskScore);
        String authenticityRisk = toRisk(authenticityScore);
        String auditStatus = decideStatus(sufficiencyLevel, authenticityRisk, aiGeneratedRisk, tamperRisk);

        ChatImageRisk risk = new ChatImageRisk();
        risk.setAuditStatus(auditStatus);
        risk.setSufficiencyLevel(sufficiencyLevel);
        risk.setAuthenticityRisk(authenticityRisk);
        risk.setAiGeneratedRisk(aiGeneratedRisk);
        risk.setTamperRisk(tamperRisk);
        risk.setMetadataSignal(joinOrDefault(metadataSignals, "未发现明确元数据异常信号，聊天预审未读取真实 EXIF/C2PA。"));
        risk.setVisualSignal(joinOrDefault(visualSignals, "图片说明与售后问题未发现明显冲突，仍需客服结合原始文件判断。"));
        risk.setWatermarkSignal(joinOrDefault(watermarkSignals, "未发现明确水印或生成平台来源信号。"));
        risk.setRequiredEvidence(joinOrDefault(required, "当前图片可作为初步材料，客服仍需结合订单和售后规则复核。"));
        risk.setRequiredEvidenceList(new ArrayList<>(required));
        risk.setSummary(summary(risk));
        return risk;
    }

    public Map<String, Object> toTraceDetail(ChatImageRisk risk) {
        if (risk == null) {
            return Map.of(
                    "title", "聊天图片风险预审",
                    "summary", "本轮未上传聊天图片，跳过图片真实性预审"
            );
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("title", "聊天图片风险预审");
        detail.put("summary", risk.getSummary());
        detail.put("auditStatus", risk.getAuditStatus());
        detail.put("sufficiencyLevel", risk.getSufficiencyLevel());
        detail.put("authenticityRisk", risk.getAuthenticityRisk());
        detail.put("aiGeneratedRisk", risk.getAiGeneratedRisk());
        detail.put("tamperRisk", risk.getTamperRisk());
        detail.put("imageRisk", risk);
        return detail;
    }

    public void attachRisks(Long sessionId, List<ChatMessage> messages) {
        if (sessionId == null || messages == null || messages.isEmpty()) {
            return;
        }
        Map<Long, ChatImageRisk> riskByMessageId = processTraceMapper.listBySessionId(sessionId).stream()
                .filter(trace -> TRACE_STEP.equals(trace.getStepName()))
                .filter(trace -> "SUCCESS".equals(trace.getStepStatus()))
                .map(this::riskEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right));
        for (ChatMessage message : messages) {
            if (message.getId() != null && riskByMessageId.containsKey(message.getId())) {
                message.setImageRisk(riskByMessageId.get(message.getId()));
            }
        }
    }

    private Map.Entry<Long, ChatImageRisk> riskEntry(ProcessTrace trace) {
        ChatImageRisk risk = parseRisk(trace);
        if (risk == null || trace.getMessageId() == null) {
            return null;
        }
        return Map.entry(trace.getMessageId(), risk);
    }

    private ChatImageRisk parseRisk(ProcessTrace trace) {
        if (trace == null || !hasText(trace.getDetailJson())) {
            return null;
        }
        try {
            Map<?, ?> detail = objectMapper.readValue(trace.getDetailJson(), Map.class);
            Object raw = detail.get("imageRisk");
            if (raw == null) {
                return null;
            }
            return objectMapper.convertValue(raw, ChatImageRisk.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean issueMatched(String text) {
        return containsAny(text, "故障", "质量", "坏", "破损", "断连", "没声音", "充电", "失灵", "照片", "图片", "外观", "物流", "快递", "少件", "包装", "配件", "退货", "换货", "退款");
    }

    private String decideStatus(String sufficiencyLevel, String authenticityRisk, String aiGeneratedRisk, String tamperRisk) {
        if ("HIGH".equals(authenticityRisk) || "HIGH".equals(aiGeneratedRisk) || "HIGH".equals(tamperRisk)) {
            return "RISKY";
        }
        if ("MEDIUM".equals(authenticityRisk) || "MEDIUM".equals(aiGeneratedRisk) || "MEDIUM".equals(tamperRisk)) {
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

    private String summary(ChatImageRisk risk) {
        if ("RISKY".equals(risk.getAuditStatus())) {
            return "聊天图片存在疑似 AI 生成、来源不清或二次处理风险，建议要求用户补充原始实拍材料并人工复核。";
        }
        if ("MANUAL_REVIEW".equals(risk.getAuditStatus())) {
            return "聊天图片存在中等风险信号，客服应结合订单、原图和故障说明人工复核。";
        }
        if ("NEED_MORE".equals(risk.getAuditStatus())) {
            return "聊天图片可作为初步材料，但说明或配套证据不足，建议补充原始照片、视频或序列号信息。";
        }
        return "聊天图片预审通过基础规则，暂未发现明显 AI 生成或篡改风险信号。";
    }

    private String joinOrDefault(Set<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return String.join("；", values);
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
        return hasText(text) && text.matches(".*(^|[^a-z0-9])ps([^a-z0-9]|$).*");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
