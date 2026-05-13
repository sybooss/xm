package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ProductIssueInsightSummary {
    private Integer days;
    private Long openCount;
    private Long highCount;
    private Long mediumCount;
    private Long productCount;
    private Long sampleCount;
    private ProductIssueAlert topAlert;
}
