package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AfterSaleApplicationMapper {
    long count(@Param("s") AfterSaleApplicationSearch search);

    List<AfterSaleApplication> page(@Param("s") AfterSaleApplicationSearch search);

    AfterSaleApplication getById(@Param("id") Long id);

    AfterSaleApplication getByTicketId(@Param("ticketId") Long ticketId);

    int countActiveByOrderId(@Param("orderId") Long orderId);

    void insert(AfterSaleApplication application);

    void updateDecision(AfterSaleApplication application);

    void bindTicket(@Param("id") Long id, @Param("ticketId") Long ticketId);
}
