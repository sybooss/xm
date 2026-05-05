package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.IntentRecord;

import java.util.List;

public interface IntentRecordMapper {
    void insert(IntentRecord record);

    List<IntentRecord> listBySessionId(Long sessionId);
}
