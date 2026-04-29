package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleRecordMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.AfterSaleRecordSearch;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.service.AfterSaleRecordService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AfterSaleRecordServiceImpl implements AfterSaleRecordService {
    @Autowired
    private AfterSaleRecordMapper recordMapper;
    @Autowired
    private DemoOrderMapper orderMapper;

    @Override
    public PageResult<AfterSaleRecord> page(AfterSaleRecordSearch search) {
        return new PageResult<>(recordMapper.count(search), recordMapper.page(search));
    }

    @Override
    public AfterSaleRecord getById(Long id) {
        AfterSaleRecord record = recordMapper.getById(id);
        if (record == null) {
            throw new BusinessException("售后记录不存在");
        }
        return record;
    }

    @Override
    public List<AfterSaleRecord> listByOrderId(Long orderId) {
        return recordMapper.listByOrderId(orderId);
    }

    @Transactional
    @Override
    public void save(AfterSaleRecord record) {
        DemoOrder order = orderMapper.getById(record.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (record.getAfterSaleNo() == null || record.getAfterSaleNo().isBlank()) {
            record.setAfterSaleNo(NoUtils.afterSaleNo());
        }
        if (record.getStatus() == null || record.getStatus().isBlank()) {
            record.setStatus("APPLIED");
        }
        recordMapper.insert(record);
        orderMapper.updateAfterSaleStatus(record.getOrderId(), toOrderAfterSaleStatus(record.getServiceType()));
    }

    @Override
    public void update(Long id, AfterSaleRecord record) {
        record.setId(id);
        recordMapper.update(record);
    }

    @Override
    public void delete(Long id) {
        recordMapper.delete(id);
    }

    private String toOrderAfterSaleStatus(String serviceType) {
        if ("EXCHANGE".equals(serviceType)) {
            return "EXCHANGE_APPLYING";
        }
        if ("REFUND".equals(serviceType)) {
            return "REFUNDING";
        }
        return "RETURN_APPLYING";
    }
}
