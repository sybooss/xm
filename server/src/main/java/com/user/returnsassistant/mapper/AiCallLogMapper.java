package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AiCallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiCallLogMapper {
    @Insert("""
            insert into ai_call_log(session_id, message_id, provider, model_name, request_summary, response_summary, status, prompt_tokens, completion_tokens, latency_ms, error_message)
            values(#{sessionId}, #{messageId}, #{provider}, #{modelName}, #{requestSummary}, #{responseSummary}, #{status}, #{promptTokens}, #{completionTokens}, #{latencyMs}, #{errorMessage})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AiCallLog log);

    @Select("""
            <script>
            select count(*) from ai_call_log
            where 1=1
            <if test="status != null and status != ''">and status=#{status}</if>
            </script>
            """)
    long count(@Param("status") String status);

    @Select("""
            <script>
            select * from ai_call_log
            where 1=1
            <if test="status != null and status != ''">and status=#{status}</if>
            order by created_at desc, id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<AiCallLog> page(@Param("status") String status, @Param("offset") Integer offset, @Param("limit") Integer limit);

    @Select("select * from ai_call_log where session_id=#{sessionId} order by created_at desc")
    List<AiCallLog> listBySessionId(Long sessionId);
}
