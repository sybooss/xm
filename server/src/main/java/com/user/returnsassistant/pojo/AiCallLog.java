package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiCallLog {
    private Long id;
    private Long sessionId;
    private Long messageId;
    private String provider;
    private String modelName;
    private String requestSummary;
    private String responseSummary;
    private String status;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer latencyMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
