package com.user.returnsassistant.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AfterSaleRecordSearch extends BaseSearch {
    private Long orderId;
    private String status;
    private String serviceType;
}
