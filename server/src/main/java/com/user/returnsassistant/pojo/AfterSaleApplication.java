package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSaleApplication {
    private Long id;
    private String applicationNo;
    private Long orderId;
    private String orderNo;
    private String productName;
    private String skuName;
    private Long userId;
    private String userDisplayName;
    private String serviceType;
    private String reasonCode;
    private String reasonText;
    private String status;
    private BigDecimal refundAmount;
    private BigDecimal approvedAmount;
    private String priority;
    private LocalDateTime slaDeadline;
    private Long assignedTo;
    private String assignedToName;
    private String aiSummary;
    private String riskLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private List<AfterSaleProcessLog> processLogs;
    private List<AfterSaleEvidence> evidences;
}
