package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDoc {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String docType;
    private String intentCode;
    private String scenario;
    private String question;
    private String answer;
    private String content;
    private String keywords;
    private Integer priority;
    private String status;
    private Integer versionNo;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
    private Double score;
    private Integer rankNo;
    private String hitReason;
    private String contentPreview;
}
