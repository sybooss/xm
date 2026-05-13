package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DemoOrderMapper {
    long count(@Param("s") OrderSearch search);

    BigDecimal sumAmount(@Param("s") OrderSearch search);

    List<DemoOrder> page(@Param("s") OrderSearch search);

    DemoOrder getById(Long id);

    DemoOrder getByOrderNo(String orderNo);

    long countAfterSalesByProductSince(@Param("productName") String productName, @Param("days") Integer days);

    void insert(DemoOrder order);

    void update(DemoOrder order);

    void updateAfterSaleStatus(@Param("id") Long id, @Param("status") String status);

    void delete(Long id);
}
