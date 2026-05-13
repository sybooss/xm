package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.mapper.ChatSessionMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.KnowledgeDocMapper;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.ProductInsight;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.service.AiBusinessToolService;
import com.user.returnsassistant.service.ProductInsightService;
import com.user.returnsassistant.service.ServiceTicketService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiBusinessToolServiceImpl implements AiBusinessToolService {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private KnowledgeDocMapper knowledgeDocMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ServiceTicketService ticketService;
    @Autowired
    private ProductInsightService productInsightService;

    @Tool("根据订单号查询订单、物流和售后状态，供售后客服模型回答时引用")
    @Override
    public String queryOrderStatus(String orderNo) {
        if (!hasText(orderNo)) {
            return toJson(Map.of("found", false, "reason", "未提供订单号"));
        }
        DemoOrder order = orderMapper.getByOrderNo(orderNo.trim());
        if (order == null) {
            return toJson(Map.of("found", false, "orderNo", orderNo, "reason", "订单不存在"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("found", true);
        data.put("orderNo", order.getOrderNo());
        data.put("productName", order.getProductName());
        data.put("orderStatus", order.getOrderStatus());
        data.put("logisticsStatus", order.getLogisticsStatus());
        data.put("afterSaleStatus", order.getAfterSaleStatus());
        data.put("signedAt", order.getSignedAt());
        return toJson(data);
    }

    @Tool("根据订单号查询商品档案，返回商品定位、参数、卖点、常见问题和售后建议")
    @Override
    public String queryProductProfile(String orderNo) {
        if (!hasText(orderNo)) {
            return toJson(Map.of("found", false, "reason", "未提供订单号"));
        }
        try {
            ProductInsight insight = productInsightService.buildByOrderNo(orderNo.trim(), "", false);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("found", insight.getHasProfile());
            data.put("matchType", insight.getMatchType());
            data.put("orderNo", insight.getOrderNo());
            data.put("productName", insight.getProductName());
            data.put("category", insight.getCategory());
            data.put("positioning", insight.getPositioning());
            data.put("specs", insight.getSpecs());
            data.put("sellingPoints", insight.getSellingPoints());
            data.put("usageScenarios", insight.getUsageScenarios());
            data.put("commonIssues", insight.getCommonIssues());
            data.put("afterSaleAdvice", insight.getAfterSaleAdvice());
            return toJson(data);
        } catch (Exception e) {
            return toJson(Map.of("found", false, "orderNo", orderNo, "reason", e.getMessage()));
        }
    }

    @Tool("结合订单商品和用户问题生成产品售后顾问摘要，供模型回答退换货咨询时引用")
    @Override
    public String generateProductInsight(String orderNo, String userIssue) {
        if (!hasText(orderNo)) {
            return toJson(Map.of("generated", false, "reason", "未提供订单号"));
        }
        try {
            ProductInsight insight = productInsightService.buildByOrderNo(orderNo.trim(), userIssue, false);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("generated", true);
            data.put("productName", insight.getProductName());
            data.put("matchedConcerns", insight.getMatchedConcerns());
            data.put("troubleshootingSteps", insight.getTroubleshootingSteps());
            data.put("comparisonText", insight.getComparisonText());
            data.put("retentionScript", insight.getRetentionScript());
            data.put("afterSaleAdvice", insight.getAfterSaleAdvice());
            data.put("localSummary", insight.getLocalSummary());
            return toJson(data);
        } catch (Exception e) {
            return toJson(Map.of("generated", false, "orderNo", orderNo, "reason", e.getMessage()));
        }
    }

    @Tool("按用户问题和售后意图检索知识库，返回可引用的规则依据")
    @Override
    public String searchAfterSaleKnowledge(String query, String intentCode) {
        List<KnowledgeDoc> docs = knowledgeDocMapper.search(query, intentCode, 3);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hitCount", docs.size());
        data.put("hits", docs.stream().map(doc -> Map.of(
                "title", nullToEmpty(doc.getTitle()),
                "intentCode", nullToEmpty(doc.getIntentCode()),
                "answer", trim(firstText(doc.getAnswer(), doc.getContent(), doc.getContentPreview()), 180)
        )).toList());
        return toJson(data);
    }

    @Tool("创建人工客服工单，用于投诉、商家不处理、物流异常升级等场景")
    @Override
    public String createServiceTicket(Long sessionId, String orderNo, String intentCode, String customerIssue) {
        ChatSession session = sessionMapper.getById(sessionId);
        if (session == null) {
            return toJson(Map.of("created", false, "reason", "会话不存在"));
        }
        DemoOrder order = hasText(orderNo) ? orderMapper.getByOrderNo(orderNo.trim()) : null;
        ServiceTicketService.TicketResult result = ticketService.createFromSession(
                session,
                order,
                null,
                hasText(intentCode) ? intentCode : "COMPLAINT_TRANSFER",
                hasText(customerIssue) ? customerIssue : "用户请求人工客服处理",
                "由 LangChain4j 业务工具整理的人工转接工单",
                "请人工客服核实订单和对话记录，联系用户确认诉求并记录处理结果。",
                false);
        ServiceTicket ticket = result.ticket();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("created", result.created());
        data.put("reason", result.reason());
        data.put("ticketNo", ticket == null ? null : ticket.getTicketNo());
        data.put("priority", ticket == null ? null : ticket.getPriority());
        data.put("status", ticket == null ? null : ticket.getStatus());
        return toJson(data);
    }

    private String firstText(String... texts) {
        for (String text : texts) {
            if (hasText(text)) {
                return text;
            }
        }
        return "";
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private String trim(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"json serialization failed\"}";
        }
    }
}
