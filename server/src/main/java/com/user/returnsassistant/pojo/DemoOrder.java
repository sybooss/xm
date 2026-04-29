package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DemoOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private String productName;
    private String skuName;
    private BigDecimal orderAmount;
    private String payStatus;
    private String orderStatus;
    private String logisticsStatus;
    private String afterSaleStatus;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AfterSaleRecord> afterSales;
}
