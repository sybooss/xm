package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServiceReview {
    private Long id;
    private Long applicationId;
    private String applicationNo;
    private Long userId;
    private String userDisplayName;
    private Integer rating;
    private String tags;
    private String comment;
    private LocalDateTime createdAt;
}
