package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.pojo.AiCallLog;
import com.user.returnsassistant.service.AiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiServiceImpl implements AiService {
    @Value("${app.ai.enabled:false}")
    private boolean aiEnabled;
    @Value("${app.ai.provider:openai-compatible}")
    private String provider;
    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String modelName;
    @Value("${app.ai.model-options:gpt-4o-mini,gpt-4.1-mini,gpt-4.1,o4-mini}")
    private String modelOptions;
    @Value("${app.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;
    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String apiKey;
    @Value("${langchain4j.open-ai.chat-model.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    @Value("${langchain4j.open-ai.chat-model.temperature:0.2}")
    private Double temperature;
    @Value("${langchain4j.open-ai.chat-model.max-retries:2}")
    private Integer maxRetries;
    @Value("${langchain4j.open-ai.chat-model.timeout-seconds:30}")
    private Integer timeoutSeconds;
    @Value("${langchain4j.open-ai.chat-model.log-requests:false}")
    private Boolean logRequests;
    @Value("${langchain4j.open-ai.chat-model.log-responses:false}")
    private Boolean logResponses;

    private volatile ChatModel chatModel;
    private volatile String activeModelName;

    @Override
    public AiResult generate(String prompt) {
        long start = System.currentTimeMillis();
        if (!aiEnabled) {
            return skipped(start, "AI 未启用，使用本地规则兜底");
        }
        if (!hasText(apiKey)) {
            return skipped(start, "未配置 OPENAI_API_KEY，使用本地规则兜底");
        }
        try {
            String reply = getChatModel().chat(prompt);
            return new AiResult(true, "SUCCESS", false, provider, currentModelName(), reply, elapsed(start), null);
        } catch (Exception e) {
            return new AiResult(false, "FAILED", fallbackEnabled, provider, currentModelName(), "", elapsed(start), summarizeError(e));
        }
    }

    @Override
    public AiResult test(String prompt) {
        long start = System.currentTimeMillis();
        String finalPrompt = hasText(prompt) ? prompt : "请用一句话说明你已经可以为电商退换货客服系统生成回复。";
        if (!aiEnabled) {
            return skipped(start, "AI 未启用");
        }
        if (!hasText(apiKey)) {
            return skipped(start, "未配置 OPENAI_API_KEY");
        }
        try {
            String reply = getChatModel().chat(finalPrompt);
            return new AiResult(true, "SUCCESS", false, provider, currentModelName(), reply, elapsed(start), null);
        } catch (Exception e) {
            return new AiResult(false, "FAILED", fallbackEnabled, provider, currentModelName(), "", elapsed(start), summarizeError(e));
        }
    }

    @Override
    public AiCallLog toLog(Long sessionId, Long messageId, String prompt, AiResult result) {
        AiCallLog log = new AiCallLog();
        log.setSessionId(sessionId);
        log.setMessageId(messageId);
        log.setProvider(result.provider());
        log.setModelName(result.modelName());
        log.setRequestSummary(trim(prompt, 4000));
        log.setResponseSummary(trim(result.reply(), 4000));
        log.setStatus(result.status());
        log.setLatencyMs(result.latencyMs());
        log.setErrorMessage(trim(result.errorMessage(), 1000));
        return log;
    }

    @Override
    public String currentModelName() {
        if (hasText(activeModelName)) {
            return activeModelName.trim();
        }
        return hasText(modelName) ? modelName.trim() : "gpt-4o-mini";
    }

    @Override
    public List<String> modelOptions() {
        Set<String> options = new LinkedHashSet<>();
        options.add(currentModelName());
        if (hasText(modelOptions)) {
            for (String item : modelOptions.split(",")) {
                String option = item.trim();
                if (hasText(option)) {
                    options.add(option);
                }
            }
        }
        return new ArrayList<>(options);
    }

    @Override
    public void switchModel(String modelName) {
        if (!hasText(modelName)) {
            throw new IllegalArgumentException("模型名不能为空");
        }
        String nextModelName = modelName.trim();
        if (nextModelName.length() > 80) {
            throw new IllegalArgumentException("模型名过长");
        }
        synchronized (this) {
            if (!nextModelName.equals(currentModelName())) {
                activeModelName = nextModelName;
                chatModel = null;
            }
        }
    }

    private ChatModel getChatModel() {
        if (chatModel == null) {
            synchronized (this) {
                if (chatModel == null) {
                    chatModel = OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .baseUrl(baseUrl)
                            .modelName(currentModelName())
                            .temperature(temperature)
                            .maxRetries(maxRetries)
                            .timeout(Duration.ofSeconds(timeoutSeconds))
                            .logRequests(logRequests)
                            .logResponses(logResponses)
                            .build();
                }
            }
        }
        return chatModel;
    }

    private AiResult skipped(long start, String message) {
        return new AiResult(false, "SKIPPED", fallbackEnabled, "local-fallback", "local-rule-template", "", elapsed(start), message);
    }

    private int elapsed(long start) {
        return (int) (System.currentTimeMillis() - start);
    }

    private String summarizeError(Exception e) {
        String message = e.getMessage();
        if (!hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return trim(message, 1000);
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
}
