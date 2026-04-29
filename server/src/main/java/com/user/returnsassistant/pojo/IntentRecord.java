package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IntentRecord {
    private Long id;
    private Long sessionId;
    private Long messageId;
    private String intentCode;
    private String intentName;
    private BigDecimal confidence;
    private String method;
    private String slotsJson;
    private LocalDateTime createdAt;
}
