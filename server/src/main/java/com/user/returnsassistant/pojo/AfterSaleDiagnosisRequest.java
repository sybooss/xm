package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleDiagnosisRequest {
    private Long orderId;
    private String orderNo;
    private Long sessionId;
    private String issueText;
    private String serviceType;
    private BigDecimal refundAmount;
    private Boolean useAi;
}
