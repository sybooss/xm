package com.user.returnsassistant.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductInsight {
    private Boolean hasProfile;
    private String matchType;
    private String orderNo;
    private String productName;
    private String skuName;
    private String category;
    private String positioning;
    private Map<String, Object> specs;
    private List<String> matchedConcerns;
    private List<String> sellingPoints;
    private List<String> usageScenarios;
    private List<String> commonIssues;
    private List<String> troubleshootingSteps;
    private String comparisonText;
    private String retentionScript;
    private String afterSaleAdvice;
    private String localSummary;
    private String aiSummary;
    private String aiStatus;
    private String aiErrorMessage;
}
