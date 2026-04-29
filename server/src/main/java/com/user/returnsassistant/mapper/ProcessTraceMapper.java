package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ProcessTrace;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProcessTraceMapper {
    @Insert("""
            insert into process_trace(session_id, message_id, step_name, step_status, detail_json)
            values(#{sessionId}, #{messageId}, #{stepName}, #{stepStatus}, #{detailJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProcessTrace trace);

    @Select("select * from process_trace where session_id=#{sessionId} order by created_at asc, id asc")
    List<ProcessTrace> listBySessionId(Long sessionId);
}
