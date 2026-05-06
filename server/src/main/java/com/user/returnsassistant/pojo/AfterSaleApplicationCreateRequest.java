package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleApplicationCreateRequest {
    private Long orderId;
    private String serviceType;
    private String reasonCode;
    private String reasonText;
    private BigDecimal refundAmount;
}
