package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class SystemController {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AiService aiService;
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;
    @Value("${app.ai.provider:openai-compatible}")
    private String aiProvider;
    @Value("${app.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;
    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;
    @Value("${langchain4j.open-ai.chat-model.base-url:}")
    private String baseUrl;

    @GetMapping("/status")
    public Result status() {
        jdbcTemplate.queryForObject("select 1", Integer.class);
        Map<String, Object> ai = aiPayload();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appName", "returns-assistant");
        data.put("database", Map.of("status", "UP", "schema", "test3"));
        data.put("ai", ai);
        return Result.success(data);
    }

    @GetMapping("/ai-models")
    public Result aiModels() {
        return Result.success(aiPayload());
    }

    @PutMapping("/ai-models/current")
    @OperatorAnno
    public Result switchAiModel(@RequestBody Map<String, String> request) {
        String modelName = request == null ? "" : request.get("modelName");
        try {
            aiService.switchModel(modelName);
            return Result.success(aiPayload());
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Map<String, Object> aiPayload() {
        Map<String, Object> ai = new LinkedHashMap<>();
        boolean apiKeyConfigured = hasText(apiKey);
        ai.put("status", aiEnabled && apiKeyConfigured ? "UP" : "SKIPPED");
        ai.put("provider", aiEnabled && apiKeyConfigured ? aiProvider : "local-fallback");
        ai.put("modelName", aiEnabled && apiKeyConfigured ? aiService.currentModelName() : "local-rule-template");
        ai.put("selectedModelName", aiService.currentModelName());
        ai.put("modelOptions", aiService.modelOptions());
        ai.put("enabled", aiEnabled);
        ai.put("apiKeyConfigured", apiKeyConfigured);
        ai.put("baseUrlConfigured", hasText(baseUrl));
        ai.put("fallbackEnabled", fallbackEnabled);
        return ai;
    }

    @GetMapping("/enums")
    public Result enums() {
        return Result.success(Map.of(
                "intentCodes", List.of(
                        Map.of("code", "PRE_SALE", "name", "售前咨询"),
                        Map.of("code", "RETURN_APPLY", "name", "退货申请"),
                        Map.of("code", "EXCHANGE_APPLY", "name", "换货申请"),
                        Map.of("code", "REFUND_PROGRESS", "name", "退款进度"),
                        Map.of("code", "LOGISTICS_QUERY", "name", "物流查询"),
                        Map.of("code", "RULE_EXPLAIN", "name", "规则说明"),
                        Map.of("code", "COMPLAINT_TRANSFER", "name", "投诉与人工转接")
                ),
                "docTypes", List.of(
                        Map.of("code", "FAQ", "name", "常见问题"),
                        Map.of("code", "POLICY", "name", "平台规则"),
                        Map.of("code", "SCRIPT", "name", "客服话术"),
                        Map.of("code", "NOTICE", "name", "通知说明")
                ),
                "ticketStatuses", List.of(
                        Map.of("code", "PENDING", "name", "待处理"),
                        Map.of("code", "PROCESSING", "name", "处理中"),
                        Map.of("code", "RESOLVED", "name", "已解决"),
                        Map.of("code", "CLOSED", "name", "已关闭")
                ),
                "ticketPriorities", List.of(
                        Map.of("code", "LOW", "name", "低"),
                        Map.of("code", "NORMAL", "name", "普通"),
                        Map.of("code", "HIGH", "name", "高"),
                        Map.of("code", "URGENT", "name", "紧急")
                )
        ));
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
