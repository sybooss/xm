package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private String messageType;
    private Integer seqNo;
    private Long replyToId;
    private String intentCode;
    private String sourceType;
    private LocalDateTime createdAt;
}
