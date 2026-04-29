package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import com.user.returnsassistant.pojo.PageResult;

public interface OrderService {
    PageResult<DemoOrder> page(OrderSearch search);

    DemoOrder getById(Long id);

    DemoOrder getByOrderNo(String orderNo);

    void save(DemoOrder order);

    void update(Long id, DemoOrder order);

    void delete(Long id);
}
