package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class SlaTaskSearch extends BaseSearch {
    private String riskType;
    private String status;
    private String priority;
}
