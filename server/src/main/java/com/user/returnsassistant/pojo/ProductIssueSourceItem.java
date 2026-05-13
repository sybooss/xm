package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductIssueSourceItem {
    private Long applicationId;
    private String applicationNo;
    private String productName;
    private String serviceType;
    private String reasonText;
    private String customerIssue;
    private String reviewTags;
    private String reviewComment;
    private Integer rating;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
}
