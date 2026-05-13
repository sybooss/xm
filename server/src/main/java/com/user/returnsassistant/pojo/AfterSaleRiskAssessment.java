package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSaleRiskAssessment {
    private Long id;
    private String assessmentNo;
    private Long applicationId;
    private String applicationNo;
    private String orderNo;
    private String productName;
    private Long userId;
    private String userDisplayName;
    private String serviceType;
    private String status;
    private String riskLevel;
    private Integer riskScore;
    private String riskTags;
    private List<String> riskTagList;
    private String riskReasons;
    private List<String> riskReasonList;
    private String suggestedAction;
    private String ruleDetailJson;
    private String aiSummary;
    private String aiStatus;
    private String aiErrorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
