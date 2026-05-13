package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSaleDiagnosis {
    private Long id;
    private String diagnosisNo;
    private Long applicationId;
    private Long sessionId;
    private Long orderId;
    private String orderNo;
    private String productName;
    private String skuName;
    private Long userId;
    private String userDisplayName;
    private String issueText;
    private String suggestedServiceType;
    private String decisionLevel;
    private String reasonSummary;
    private String requiredEvidence;
    private List<String> requiredEvidenceList;
    private String solutionOptionsJson;
    private List<AfterSaleSolutionOption> solutionOptions;
    private String aiSummary;
    private String aiStatus;
    private String aiErrorMessage;
    private LocalDateTime createdAt;
}
