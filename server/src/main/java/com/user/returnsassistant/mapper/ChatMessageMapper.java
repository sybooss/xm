package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatMessageMapper {
    List<ChatMessage> listBySessionId(Long sessionId);

    List<ChatMessage> listRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    int maxSeqNo(Long sessionId);

    void insert(ChatMessage message);
}
