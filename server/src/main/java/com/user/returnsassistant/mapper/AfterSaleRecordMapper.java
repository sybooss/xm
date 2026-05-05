package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.AfterSaleRecordSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AfterSaleRecordMapper {
    long count(@Param("s") AfterSaleRecordSearch search);

    List<AfterSaleRecord> page(@Param("s") AfterSaleRecordSearch search);

    AfterSaleRecord getById(Long id);

    List<AfterSaleRecord> listByOrderId(Long orderId);

    void insert(AfterSaleRecord record);

    void update(AfterSaleRecord record);

    void delete(Long id);
}
