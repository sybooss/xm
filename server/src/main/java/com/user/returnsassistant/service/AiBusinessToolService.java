package com.user.returnsassistant.service;

public interface AiBusinessToolService {
    String queryOrderStatus(String orderNo);

    String queryProductProfile(String orderNo);

    String generateProductInsight(String orderNo, String userIssue);

    String searchAfterSaleKnowledge(String query, String intentCode);

    String createServiceTicket(Long sessionId, String orderNo, String intentCode, String customerIssue);
}
