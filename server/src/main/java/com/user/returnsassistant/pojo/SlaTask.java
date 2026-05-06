package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SlaTask {
    private Long id;
    private String applicationNo;
    private Long orderId;
    private String orderNo;
    private String productName;
    private Long userId;
    private String userDisplayName;
    private String serviceType;
    private String status;
    private String priority;
    private String riskLevel;
    private BigDecimal refundAmount;
    private LocalDateTime slaDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String riskLabel;
    private Long remainingHours;
}
