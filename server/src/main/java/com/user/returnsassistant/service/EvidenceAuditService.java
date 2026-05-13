package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AfterSaleEvidence;
import com.user.returnsassistant.pojo.EvidenceAudit;
import com.user.returnsassistant.pojo.EvidenceAuditRequest;
import com.user.returnsassistant.pojo.UserAccount;

import java.util.List;

public interface EvidenceAuditService {
    EvidenceAudit audit(Long evidenceId, EvidenceAuditRequest request, UserAccount user);

    List<EvidenceAudit> listByEvidenceId(Long evidenceId, UserAccount user);

    List<EvidenceAudit> listByApplicationId(Long applicationId, UserAccount user);

    void attachLatestAudits(Long applicationId, List<AfterSaleEvidence> evidences);
}
