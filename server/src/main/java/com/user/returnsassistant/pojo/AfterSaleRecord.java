package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleRecord {
    private Long id;
    private String afterSaleNo;
    private Long orderId;
    private String orderNo;
    private String serviceType;
    private String reason;
    private String status;
    private BigDecimal refundAmount;
    private LocalDateTime applyAt;
    private LocalDateTime handleAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
