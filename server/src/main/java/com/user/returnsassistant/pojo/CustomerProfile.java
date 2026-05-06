package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CustomerProfile {
    private UserAccount customer;
    private Long orderCount;
    private BigDecimal totalOrderAmount;
    private Long afterSaleCount;
    private Long activeAfterSaleCount;
    private Long ticketCount;
    private Long reviewCount;
    private Double averageRating;
    private String riskLevel;
    private List<DemoOrder> recentOrders;
    private List<AfterSaleApplication> recentAfterSales;
    private List<ServiceTicket> recentTickets;
    private List<ServiceReview> reviews;
}
