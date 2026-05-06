package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleEvidence {
    private Long id;
    private Long applicationId;
    private String evidenceType;
    private String fileUrl;
    private String content;
    private Long uploadedBy;
    private String uploadedByName;
    private LocalDateTime createdAt;
}
