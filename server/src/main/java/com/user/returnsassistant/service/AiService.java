package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AiCallLog;

import java.util.List;

public interface AiService {
    AiResult generate(String prompt);

    AiResult test(String prompt);

    AiCallLog toLog(Long sessionId, Long messageId, String prompt, AiResult result);

    String currentModelName();

    List<String> modelOptions();

    void switchModel(String modelName);

    record AiResult(boolean used, String status, boolean fallbackUsed, String provider, String modelName,
                    String reply, int latencyMs, String errorMessage) {
    }
}
