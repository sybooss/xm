package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.ProductInsight;

public interface ProductInsightService {
    ProductInsight buildByOrderId(Long orderId, String userIssue, Boolean useAi);

    ProductInsight buildByOrderNo(String orderNo, String userIssue, Boolean useAi);

    ProductInsight build(DemoOrder order, String userIssue, String intentCode, Boolean useAi);
}
