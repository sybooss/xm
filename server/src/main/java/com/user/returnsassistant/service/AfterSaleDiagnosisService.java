package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AfterSaleDiagnosis;
import com.user.returnsassistant.pojo.AfterSaleDiagnosisRequest;
import com.user.returnsassistant.pojo.UserAccount;

public interface AfterSaleDiagnosisService {
    AfterSaleDiagnosis diagnose(AfterSaleDiagnosisRequest request, UserAccount user);

    AfterSaleDiagnosis getById(Long id, UserAccount user);

    AfterSaleDiagnosis getLatestBySessionId(Long sessionId, UserAccount user);

    AfterSaleDiagnosis getInternal(Long id);

    AfterSaleDiagnosis getOwnedForBinding(Long id, Long orderId, UserAccount user);

    void bindApplication(Long diagnosisId, Long applicationId);
}
