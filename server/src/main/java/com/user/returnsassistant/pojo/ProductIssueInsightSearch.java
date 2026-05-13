package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ProductIssueInsightSearch extends BaseSearch {
    private Integer days = 7;
    private String alertLevel;
    private String status = "OPEN";
}
