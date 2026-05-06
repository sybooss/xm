package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.ServiceTicketSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceTicketMapper {
    long count(@Param("s") ServiceTicketSearch search);

    List<ServiceTicket> page(@Param("s") ServiceTicketSearch search);

    ServiceTicket getById(Long id);

    List<ServiceTicket> listBySessionId(Long sessionId);

    List<ServiceTicket> listByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    long countByUserId(@Param("userId") Long userId);

    ServiceTicket getOpenBySessionId(Long sessionId);

    void insert(ServiceTicket ticket);

    void update(ServiceTicket ticket);

    void delete(Long id);
}
