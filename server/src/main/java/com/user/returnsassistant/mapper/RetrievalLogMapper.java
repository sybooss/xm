package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.RetrievalLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RetrievalLogMapper {
    void insert(RetrievalLog log);

    List<RetrievalLog> listBySessionId(Long sessionId);

    long count(@Param("keyword") String keyword);

    List<RetrievalLog> page(@Param("keyword") String keyword);
}
