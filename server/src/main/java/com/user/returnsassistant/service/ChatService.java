package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.*;

import java.util.List;
import java.util.Map;

public interface ChatService {
    PageResult<ChatSession> page(ChatSessionSearch search);

    ChatSession save(ChatSession session);

    Map<String, Object> getDetail(Long id);

    void update(Long id, ChatSession session);

    void delete(Long id);

    List<ChatMessage> listMessages(Long id);

    Map<String, Object> sendMessage(Long id, ChatMessageRequest request);

    List<ProcessTrace> listTraces(Long id);
}
