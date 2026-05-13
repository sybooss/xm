package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AfterSaleRiskAssessment;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentRequest;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentSearch;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.UserAccount;

public interface AfterSaleRiskAssessmentService {
    AfterSaleRiskAssessment assess(Long applicationId, AfterSaleRiskAssessmentRequest request, UserAccount admin);

    AfterSaleRiskAssessment getByApplicationId(Long applicationId, UserAccount admin);

    PageResult<AfterSaleRiskAssessment> page(AfterSaleRiskAssessmentSearch search);
}
