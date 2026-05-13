package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductIssueAlert {
    private Long id;
    private String alertNo;
    private String productName;
    private String issueKeyword;
    private Integer issueCount;
    private Integer applicationCount;
    private Integer ticketCount;
    private Integer lowRatingCount;
    private BigDecimal refundAmount;
    private Integer timeWindowDays;
    private String alertLevel;
    private Integer trendScore;
    private String sampleApplicationIds;
    private List<Long> sampleApplicationIdList;
    private String sampleReasons;
    private List<String> sampleReasonList;
    private String suggestedAction;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
