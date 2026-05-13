package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String content;
    private String orderNo;
    private Boolean useAi = true;
    private String fileUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
}
