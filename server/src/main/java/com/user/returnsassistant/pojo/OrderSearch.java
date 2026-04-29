package com.user.returnsassistant.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderSearch extends BaseSearch {
    private String orderStatus;
    private String logisticsStatus;
    private String afterSaleStatus;
}
