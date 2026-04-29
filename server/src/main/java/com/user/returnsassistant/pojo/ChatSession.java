package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatSession {
    private Long id;
    private String sessionNo;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private String title;
    private String channel;
    private String status;
    private String currentIntent;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private DemoOrder order;
    private List<ChatMessage> messages;
}
