package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AiCallLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiCallLogMapper {
    void insert(AiCallLog log);

    long count(@Param("status") String status);

    List<AiCallLog> page(@Param("status") String status);

    List<AiCallLog> listBySessionId(Long sessionId);
}
