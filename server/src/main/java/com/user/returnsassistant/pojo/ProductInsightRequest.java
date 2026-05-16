package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ProductInsightRequest {
    private Long orderId;
    private String orderNo;
    private String issueText;
    private Boolean useAi = true;
}
