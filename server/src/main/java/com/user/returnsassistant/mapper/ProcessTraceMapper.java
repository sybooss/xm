package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ProcessTrace;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProcessTraceMapper {
    void insert(ProcessTrace trace);

    List<ProcessTrace> listBySessionId(Long sessionId);

    List<ProcessTrace> listRecent(@Param("limit") Integer limit);
}
