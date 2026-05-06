package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReplyDraft {
    private Long id;
    private Long applicationId;
    private Long ticketId;
    private String draftContent;
    private String sourceType;
    private String status;
    private String riskLevel;
    private String knowledgeRefs;
    private String aiStatus;
    private String aiProvider;
    private String aiModelName;
    private String auditRemark;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
