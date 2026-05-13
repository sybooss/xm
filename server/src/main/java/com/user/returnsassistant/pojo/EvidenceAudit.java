package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EvidenceAudit {
    private Long id;
    private String auditNo;
    private Long applicationId;
    private Long evidenceId;
    private String evidenceType;
    private String evidenceContent;
    private String auditStatus;
    private String sufficiencyLevel;
    private String authenticityRisk;
    private String aiGeneratedRisk;
    private String tamperRisk;
    private String metadataSignal;
    private String visualSignal;
    private String watermarkSignal;
    private String requiredEvidence;
    private List<String> requiredEvidenceList;
    private String auditDetailJson;
    private String aiSummary;
    private String aiStatus;
    private String aiErrorMessage;
    private LocalDateTime createdAt;
}
