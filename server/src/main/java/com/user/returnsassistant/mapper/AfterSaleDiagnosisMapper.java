package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleDiagnosis;
import org.apache.ibatis.annotations.Param;

public interface AfterSaleDiagnosisMapper {
    AfterSaleDiagnosis getById(@Param("id") Long id);

    AfterSaleDiagnosis getLatestBySessionId(@Param("sessionId") Long sessionId);

    void insert(AfterSaleDiagnosis diagnosis);

    void bindApplication(@Param("id") Long id, @Param("applicationId") Long applicationId);
}
