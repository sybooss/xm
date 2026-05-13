package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class AfterSaleRiskAssessmentSearch extends BaseSearch {
    private String riskLevel;
    private String status;
    private String serviceType;
}
