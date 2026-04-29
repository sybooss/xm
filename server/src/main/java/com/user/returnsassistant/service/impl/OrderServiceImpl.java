package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleRecordMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private AfterSaleRecordMapper afterSaleRecordMapper;

    @Override
    public PageResult<DemoOrder> page(OrderSearch search) {
        return new PageResult<>(orderMapper.count(search), orderMapper.page(search));
    }

    @Override
    public DemoOrder getById(Long id) {
        DemoOrder order = orderMapper.getById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setAfterSales(afterSaleRecordMapper.listByOrderId(id));
        return order;
    }

    @Override
    public DemoOrder getByOrderNo(String orderNo) {
        DemoOrder order = orderMapper.getByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        order.setAfterSales(afterSaleRecordMapper.listByOrderId(order.getId()));
        return order;
    }

    @Override
    public void save(DemoOrder order) {
        if (order.getAfterSaleStatus() == null) {
            order.setAfterSaleStatus("NONE");
        }
        orderMapper.insert(order);
    }

    @Override
    public void update(Long id, DemoOrder order) {
        order.setId(id);
        orderMapper.update(order);
    }

    @Override
    public void delete(Long id) {
        orderMapper.delete(id);
    }
}
