package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ProcessTrace;

import java.util.List;

public interface ProcessTraceMapper {
    void insert(ProcessTrace trace);

    List<ProcessTrace> listBySessionId(Long sessionId);
}
