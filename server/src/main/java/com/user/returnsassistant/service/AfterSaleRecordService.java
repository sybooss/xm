package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.AfterSaleRecordSearch;
import com.user.returnsassistant.pojo.PageResult;

import java.util.List;

public interface AfterSaleRecordService {
    PageResult<AfterSaleRecord> page(AfterSaleRecordSearch search);

    AfterSaleRecord getById(Long id);

    List<AfterSaleRecord> listByOrderId(Long orderId);

    void save(AfterSaleRecord record);

    void update(Long id, AfterSaleRecord record);

    void delete(Long id);
}
