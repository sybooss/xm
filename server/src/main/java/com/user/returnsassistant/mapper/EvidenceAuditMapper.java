package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.EvidenceAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EvidenceAuditMapper {
    EvidenceAudit getById(@Param("id") Long id);

    EvidenceAudit getLatestByEvidenceId(@Param("evidenceId") Long evidenceId);

    List<EvidenceAudit> listByEvidenceId(@Param("evidenceId") Long evidenceId);

    List<EvidenceAudit> listByApplicationId(@Param("applicationId") Long applicationId);

    List<EvidenceAudit> listLatestByApplicationId(@Param("applicationId") Long applicationId);

    void insert(EvidenceAudit audit);
}
