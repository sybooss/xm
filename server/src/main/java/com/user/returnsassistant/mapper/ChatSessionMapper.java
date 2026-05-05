package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.ChatSessionSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatSessionMapper {
    long count(@Param("s") ChatSessionSearch search);

    List<ChatSession> page(@Param("s") ChatSessionSearch search);

    ChatSession getById(Long id);

    void insert(ChatSession session);

    void update(ChatSession session);

    void close(Long id);

    void bindOrder(@Param("id") Long id, @Param("orderId") Long orderId);

    void updateSummary(@Param("id") Long id, @Param("intentCode") String intentCode, @Param("summary") String summary);

    void delete(Long id);
}
