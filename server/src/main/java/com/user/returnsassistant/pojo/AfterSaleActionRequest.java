package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleActionRequest {
    private String remark;
    private BigDecimal approvedAmount;
    private Long assignedTo;
}
