package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductProfile {
    private Long id;
    private String productName;
    private String productAlias;
    private String category;
    private String positioning;
    private String specJson;
    private String sellingPoints;
    private String usageScenarios;
    private String commonIssues;
    private String troubleshootingSteps;
    private String comparisonText;
    private String retentionScript;
    private String afterSaleAdvice;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
