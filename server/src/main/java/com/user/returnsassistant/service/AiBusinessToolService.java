package com.user.returnsassistant.service;

public interface AiBusinessToolService {
    String queryOrderStatus(String orderNo);

    String searchAfterSaleKnowledge(String query, String intentCode);

    String createServiceTicket(Long sessionId, String orderNo, String intentCode, String customerIssue);
}
