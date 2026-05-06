package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class AfterSaleApplicationSearch extends BaseSearch {
    private Long userId;
    private Long orderId;
    private String status;
    private String serviceType;
    private String priority;
    private Long assignedTo;
}
