package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
import java.util.Objects;

@Service
public class AfterSaleRecordServiceImpl implements AfterSaleRecordService {
    @Autowired
    private AfterSaleRecordMapper recordMapper;
    @Autowired
    private DemoOrderMapper orderMapper;

    @Override
    public PageResult<AfterSaleRecord> page(AfterSaleRecordSearch search) {
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<AfterSaleRecord> page = (Page<AfterSaleRecord>) recordMapper.page(search);
        return new PageResult<>(page.getTotal(), page.getResult());
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

    @Transactional
    @Override
    public void update(Long id, AfterSaleRecord record) {
        AfterSaleRecord old = getById(id);
        record.setId(id);
        if (record.getOrderId() == null) {
            record.setOrderId(old.getOrderId());
        }
        if (record.getAfterSaleNo() == null || record.getAfterSaleNo().isBlank()) {
            record.setAfterSaleNo(old.getAfterSaleNo());
        }
        if (record.getServiceType() == null || record.getServiceType().isBlank()) {
            record.setServiceType(old.getServiceType());
        }
        if (record.getStatus() == null || record.getStatus().isBlank()) {
            record.setStatus(old.getStatus());
        }
        recordMapper.update(record);
        syncOrderAfterSaleStatus(old.getOrderId());
        if (!Objects.equals(old.getOrderId(), record.getOrderId())) {
            syncOrderAfterSaleStatus(record.getOrderId());
        }
    }

    @Transactional
    @Override
    public void delete(Long id) {
        AfterSaleRecord old = getById(id);
        recordMapper.delete(id);
        syncOrderAfterSaleStatus(old.getOrderId());
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

    private void syncOrderAfterSaleStatus(Long orderId) {
        if (orderId == null) {
            return;
        }
        List<AfterSaleRecord> records = recordMapper.listByOrderId(orderId);
        if (records.isEmpty()) {
            orderMapper.updateAfterSaleStatus(orderId, "NONE");
            return;
        }
        AfterSaleRecord latest = records.get(0);
        String status = switch (latest.getStatus()) {
            case "REJECTED" -> "REJECTED";
            case "FINISHED" -> "FINISHED";
            case "REFUNDING" -> "REFUNDING";
            default -> toOrderAfterSaleStatus(latest.getServiceType());
        };
        orderMapper.updateAfterSaleStatus(orderId, status);
    }
}
