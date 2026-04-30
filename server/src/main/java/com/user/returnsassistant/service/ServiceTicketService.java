package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.*;

import java.util.List;

public interface ServiceTicketService {
    PageResult<ServiceTicket> page(ServiceTicketSearch search);

    ServiceTicket getById(Long id);

    List<ServiceTicket> listBySessionId(Long sessionId);

    ServiceTicket save(ServiceTicket ticket);

    void update(Long id, ServiceTicket ticket);

    void delete(Long id);

    TicketResult createFromSession(ChatSession session, DemoOrder order, Long messageId, String intentCode,
                                   String customerIssue, String aiSummary, String suggestedAction, boolean forceNew);

    record TicketResult(boolean created, ServiceTicket ticket, String reason) {
    }
}
