package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProcessTrace {
    private Long id;
    private Long sessionId;
    private Long messageId;
    private String stepName;
    private String stepStatus;
    private String detailJson;
    private LocalDateTime createdAt;
}
