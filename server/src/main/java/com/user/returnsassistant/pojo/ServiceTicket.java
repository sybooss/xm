package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceTicket {
    private Long id;
    private String ticketNo;
    private Long sessionId;
    private String sessionNo;
    private Long messageId;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String intentCode;
    private String priority;
    private String status;
    private String customerIssue;
    private String aiSummary;
    private String suggestedAction;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Integer deleted;
}
