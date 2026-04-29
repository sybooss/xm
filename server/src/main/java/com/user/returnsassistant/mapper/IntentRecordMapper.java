package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.IntentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IntentRecordMapper {
    @Insert("""
            insert into intent_record(session_id, message_id, intent_code, intent_name, confidence, method, slots_json)
            values(#{sessionId}, #{messageId}, #{intentCode}, #{intentName}, #{confidence}, #{method}, #{slotsJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(IntentRecord record);

    @Select("select * from intent_record where session_id=#{sessionId} order by created_at desc")
    List<IntentRecord> listBySessionId(Long sessionId);
}
