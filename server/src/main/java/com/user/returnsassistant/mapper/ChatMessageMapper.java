package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    @Select("select * from chat_message where session_id=#{sessionId} order by seq_no asc")
    List<ChatMessage> listBySessionId(Long sessionId);

    @Select("""
            select * from (
                select * from chat_message
                where session_id=#{sessionId}
                order by seq_no desc
                limit #{limit}
            ) recent
            order by seq_no asc
            """)
    List<ChatMessage> listRecentBySessionId(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    @Select("select coalesce(max(seq_no), 0) from chat_message where session_id=#{sessionId}")
    int maxSeqNo(Long sessionId);

    @Insert("""
            insert into chat_message(session_id, role, content, message_type, seq_no, reply_to_id, intent_code, source_type)
            values(#{sessionId}, #{role}, #{content}, coalesce(#{messageType}, 'TEXT'), #{seqNo}, #{replyToId}, #{intentCode}, #{sourceType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatMessage message);
}
