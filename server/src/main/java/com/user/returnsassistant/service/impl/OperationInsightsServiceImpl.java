package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.pojo.OperationInsights;
import com.user.returnsassistant.service.OperationInsightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OperationInsightsServiceImpl implements OperationInsightsService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public OperationInsights getInsights() {
        OperationInsights insights = new OperationInsights();
        insights.setGeneratedAt(LocalDateTime.now());
        insights.setMetrics(buildMetrics());
        insights.setNewFeatures(buildNewFeatures());
        insights.setIntentInsights(buildIntentInsights());
        insights.setTicketInsights(buildTicketInsights());
        insights.setChannelInsights(buildChannelInsights());
        insights.setKnowledgeInsights(buildKnowledgeInsights());
        insights.setOrderRiskInsights(buildOrderRiskInsights());
        insights.setAiInsights(buildAiInsights());
        insights.setActionItems(buildActionItems());
        insights.setVersionMilestones(buildVersionMilestones());
        return insights;
    }

    private List<OperationInsights.InsightMetric> buildMetrics() {
        int sessions = count("select count(*) from chat_session");
        int tickets = count("select count(*) from service_ticket where deleted = 0");
        int pendingTickets = count("select count(*) from service_ticket where deleted = 0 and status in ('PENDING', 'PROCESSING')");
        int knowledgeHits = count("select count(*) from retrieval_log");
        int aiCalls = count("select count(*) from ai_call_log");
        int successCalls = count("select count(*) from ai_call_log where status = 'SUCCESS'");
        String aiRate = percentLabel(successCalls, aiCalls);
        return List.of(
                new OperationInsights.InsightMetric("新增特色功能", "12 项", "本轮新增售后运营指挥中心，集中承载 12 个运营闭环能力。", "primary"),
                new OperationInsights.InsightMetric("会话样本", sessions + " 个", "用于意图分布、渠道分析和客户问题热度判断。", "info"),
                new OperationInsights.InsightMetric("待处理工单", pendingTickets + " 个", "待处理和处理中工单会进入 SLA 风险队列。", pendingTickets > 0 ? "warning" : "success"),
                new OperationInsights.InsightMetric("知识命中", knowledgeHits + " 次", "用于判断哪些规则被频繁检索，反向优化知识库。", "success"),
                new OperationInsights.InsightMetric("AI 成功率", aiRate, "按 AI 调用日志聚合成功、失败和跳过状态。", successCalls == aiCalls && aiCalls > 0 ? "success" : "warning"),
                new OperationInsights.InsightMetric("人工闭环量", tickets + " 单", "投诉、物流异常和人工客服请求形成可跟踪工单。", "primary")
        );
    }

    private List<OperationInsights.FeatureCard> buildNewFeatures() {
        return List.of(
                feature(1, "运营指挥中心", "总控", "本轮新增", "把售后会话、工单、知识、AI 和订单风险聚合到一个可演示页面。", "/operations", "GET /operation-insights", "运营页首屏指标与功能板", "浏览器断言“新增特色功能”和“运营指挥中心”"),
                feature(2, "意图热力雷达", "分析", "本轮新增", "按意图聚合用户问题，快速看出退货、退款、物流、投诉热点。", "/operations", "GET /operation-insights", "intentInsights", "接口返回 intentInsights 列表"),
                feature(3, "工单 SLA 风险队列", "工单", "本轮新增", "根据工单状态和优先级识别紧急、待处理和处理中风险。", "/operations", "GET /operation-insights", "ticketInsights", "页面展示 SLA 风险标签"),
                feature(4, "多渠道会话分布", "渠道", "本轮新增", "统计 WEB、ADMIN_TEST 等入口，为后续网页/App/小程序接入预留演示位。", "/operations", "GET /operation-insights", "channelInsights", "页面展示渠道占比"),
                feature(5, "知识命中 Top 榜", "知识库", "本轮新增", "统计最常被命中的知识文档和平均相关度，指导补充规则。", "/operations", "GET /operation-insights", "knowledgeInsights", "页面展示知识命中文档"),
                feature(6, "订单风险扫描", "订单", "本轮新增", "扫描物流异常、退款中、退换货处理中订单，给出运营处置建议。", "/operations", "GET /operation-insights", "orderRiskInsights", "页面展示订单风险原因"),
                feature(7, "AI 运行质量摘要", "AI", "本轮新增", "聚合 AI 成功率、平均耗时、失败数和跳过数，便于答辩解释兜底机制。", "/operations", "GET /operation-insights", "aiInsights", "页面展示 AI 成功率"),
                feature(8, "下一步动作清单", "运营", "本轮新增", "把高风险工单、知识短板、AI 失败等转成明确待办动作。", "/operations", "GET /operation-insights", "actionItems", "页面展示 owner 和 priority"),
                feature(9, "答辩亮点矩阵", "展示", "本轮新增", "将 12 个新增特色功能按类别、接口、证据、验收方式集中展示。", "/operations", "GET /operation-insights", "newFeatures", "浏览器断言“答辩亮点矩阵”"),
                feature(10, "版本里程碑面板", "版本", "本轮新增", "把每轮新增功能、验证与 Git 版本管理节奏放到可视化路线里。", "/operations", "GET /operation-insights", "versionMilestones", "页面展示 V2.0 里程碑"),
                feature(11, "外部项目借鉴落点", "工程", "本轮新增", "在文档和页面中说明参考 lilishop、LangChain4j Petclinic、KMatrix 等项目后的本地落点。", "/operations", "docs/feature-roadmap.md", "参考项目表", "文档记录参考对象和借鉴点"),
                feature(12, "闭环验收证据索引", "验收", "本轮新增", "每个新增特色功能都绑定入口、接口、证据和测试口径。", "/operations", "GET /operation-insights", "evidence/validation 字段", "接口与浏览器测试共同覆盖")
        );
    }

    private List<OperationInsights.IntentInsight> buildIntentInsights() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select intent_code, count(*) as total
                from intent_record
                group by intent_code
                order by total desc
                limit 6
                """);
        int total = rows.stream().mapToInt(row -> number(row.get("total"))).sum();
        if (rows.isEmpty()) {
            return List.of(new OperationInsights.IntentInsight("WAITING", "等待样本", 0, "0%", "先完成一次咨询，让系统生成意图记录。"));
        }
        return rows.stream()
                .map(row -> {
                    String code = string(row.get("intent_code"), "UNKNOWN");
                    int count = number(row.get("total"));
                    return new OperationInsights.IntentInsight(code, intentName(code), count, percentLabel(count, total), intentSuggestion(code));
                })
                .toList();
    }

    private List<OperationInsights.TicketInsight> buildTicketInsights() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select status, count(*) as total
                from service_ticket
                where deleted = 0
                group by status
                order by total desc
                """);
        if (rows.isEmpty()) {
            return List.of(new OperationInsights.TicketInsight("EMPTY", 0, "暂无工单", "完成一次投诉或转人工咨询后，这里会形成处理队列。"));
        }
        return rows.stream()
                .map(row -> {
                    String status = string(row.get("status"), "UNKNOWN");
                    int count = number(row.get("total"));
                    return new OperationInsights.TicketInsight(status, count, ticketRisk(status, count), ticketAction(status));
                })
                .toList();
    }

    private List<OperationInsights.ChannelInsight> buildChannelInsights() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select channel, count(*) as total
                from chat_session
                group by channel
                order by total desc
                """);
        int total = rows.stream().mapToInt(row -> number(row.get("total"))).sum();
        if (rows.isEmpty()) {
            return List.of(new OperationInsights.ChannelInsight("WEB", 0, "0%", "默认网页咨询入口，等待会话样本。"));
        }
        return rows.stream()
                .map(row -> {
                    String channel = string(row.get("channel"), "WEB");
                    int count = number(row.get("total"));
                    return new OperationInsights.ChannelInsight(channel, count, percentLabel(count, total), channelScenario(channel));
                })
                .toList();
    }

    private List<OperationInsights.KnowledgeInsight> buildKnowledgeInsights() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select coalesce(doc_title_snapshot, concat('文档#', doc_id)) as title,
                       count(*) as hit_count,
                       avg(coalesce(score, 0)) as avg_score
                from retrieval_log
                group by coalesce(doc_title_snapshot, concat('文档#', doc_id))
                order by hit_count desc, avg_score desc
                limit 5
                """);
        if (rows.isEmpty()) {
            return List.of(new OperationInsights.KnowledgeInsight("等待知识命中", 0, BigDecimal.ZERO, "0.00", "先在咨询工作台或知识库检索中触发一次规则命中。"));
        }
        return rows.stream()
                .map(row -> {
                    BigDecimal averageScore = decimal(row.get("avg_score"));
                    int hitCount = number(row.get("hit_count"));
                    return new OperationInsights.KnowledgeInsight(
                            string(row.get("title"), "未命名文档"),
                            hitCount,
                            averageScore,
                            averageScore.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                            hitCount >= 3 ? "沉淀为答辩高频问题" : "继续观察命中稳定性"
                    );
                })
                .toList();
    }

    private List<OperationInsights.OrderRiskInsight> buildOrderRiskInsights() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select order_no, product_name, logistics_status, after_sale_status
                from demo_order
                where logistics_status = 'ABNORMAL'
                   or after_sale_status in ('RETURN_APPLYING', 'RETURNING', 'EXCHANGE_APPLYING', 'REFUNDING')
                order by updated_at desc
                limit 6
                """);
        if (rows.isEmpty()) {
            return List.of(new OperationInsights.OrderRiskInsight("暂无风险订单", "-", "健康", "当前没有物流异常或售后处理中订单。", "保持订单管理页数据巡检。"));
        }
        return rows.stream()
                .map(row -> {
                    String logistics = string(row.get("logistics_status"), "NORMAL");
                    String afterSale = string(row.get("after_sale_status"), "NONE");
                    String riskType = "ABNORMAL".equals(logistics) ? "物流异常" : "售后处理中";
                    String reason = "ABNORMAL".equals(logistics)
                            ? "物流状态异常，建议主动安抚并同步承运商进度。"
                            : "售后状态为 " + afterSale + "，需要关注处理时效。";
                    return new OperationInsights.OrderRiskInsight(
                            string(row.get("order_no"), "-"),
                            string(row.get("product_name"), "-"),
                            riskType,
                            reason,
                            "进入订单管理页核对订单上下文，并在必要时转人工工单。"
                    );
                })
                .toList();
    }

    private List<OperationInsights.AiInsight> buildAiInsights() {
        int total = count("select count(*) from ai_call_log");
        int success = count("select count(*) from ai_call_log where status = 'SUCCESS'");
        int failed = count("select count(*) from ai_call_log where status = 'FAILED'");
        int skipped = count("select count(*) from ai_call_log where status = 'SKIPPED'");
        Number avgLatency = jdbcTemplate.queryForObject("select coalesce(avg(latency_ms), 0) from ai_call_log", Number.class);
        return List.of(
                new OperationInsights.AiInsight("成功率", percentLabel(success, total), "成功 " + success + " 次 / 总计 " + total + " 次", total > 0 && failed == 0 ? "success" : "warning"),
                new OperationInsights.AiInsight("平均耗时", number(avgLatency) + " ms", "用于判断模型接入是否影响客服体验。", "info"),
                new OperationInsights.AiInsight("失败次数", failed + " 次", "失败时系统应自动回落本地规则。", failed > 0 ? "warning" : "success"),
                new OperationInsights.AiInsight("兜底次数", skipped + " 次", "AI 未启用或跳过时仍保持业务可用。", skipped > 0 ? "primary" : "info")
        );
    }

    private List<OperationInsights.ActionItem> buildActionItems() {
        int pendingTickets = count("select count(*) from service_ticket where deleted = 0 and status in ('PENDING', 'PROCESSING')");
        int failedAi = count("select count(*) from ai_call_log where status = 'FAILED'");
        int abnormalOrders = count("select count(*) from demo_order where logistics_status = 'ABNORMAL'");
        int lowKnowledge = count("select count(*) from retrieval_log");
        return List.of(
                new OperationInsights.ActionItem("处理高风险工单", "客服主管", pendingTickets > 0 ? "P0" : "P2", "当前有 " + pendingTickets + " 个待处理或处理中工单，优先关闭投诉类问题。", "/service-tickets"),
                new OperationInsights.ActionItem("复核物流异常订单", "售后运营", abnormalOrders > 0 ? "P1" : "P3", "当前有 " + abnormalOrders + " 个物流异常订单，建议主动同步进度。", "/orders"),
                new OperationInsights.ActionItem("补充高频知识规则", "知识库维护", lowKnowledge < 5 ? "P1" : "P2", "知识命中样本为 " + lowKnowledge + " 条，样本不足时应增加演示 FAQ。", "/knowledge"),
                new OperationInsights.ActionItem("观察 AI 失败原因", "系统管理员", failedAi > 0 ? "P1" : "P3", "AI 失败 " + failedAi + " 次，失败时检查模型网关和本地兜底记录。", "/logs")
        );
    }

    private List<OperationInsights.VersionMilestone> buildVersionMilestones() {
        return List.of(
                new OperationInsights.VersionMilestone("V1.0", "2026-04-30", "完成基础售后闭环、流式客服、RAG、工具调用、权限和日志。", true),
                new OperationInsights.VersionMilestone("V2.0", "2026-05-06", "本轮新增售后运营指挥中心，一次性落地 12 个特色功能闭环。", true),
                new OperationInsights.VersionMilestone("V2.1", "下一轮", "把多渠道字段从模拟统计升级为真实筛选和创建入口。", false),
                new OperationInsights.VersionMilestone("V2.2", "后续", "加入导出报告、SLA 自动提醒和可配置运营规则。", false)
        );
    }

    private OperationInsights.FeatureCard feature(Integer sequence, String title, String category, String status, String detail, String route, String endpoint, String evidence, String validation) {
        return new OperationInsights.FeatureCard(sequence, title, category, status, detail, route, endpoint, evidence, validation);
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return safe(value);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private String percentLabel(int count, int total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(count * 100.0 / total) + "%";
    }

    private String intentName(String code) {
        return switch (code) {
            case "PRE_SALE" -> "售前咨询";
            case "RETURN_APPLY" -> "退货申请";
            case "EXCHANGE_APPLY" -> "换货申请";
            case "REFUND_PROGRESS" -> "退款进度";
            case "LOGISTICS_QUERY" -> "物流查询";
            case "RULE_EXPLAIN" -> "规则说明";
            case "COMPLAINT_TRANSFER" -> "投诉与人工转接";
            default -> "未知意图";
        };
    }

    private String intentSuggestion(String code) {
        return switch (code) {
            case "RETURN_APPLY" -> "补充退货条件、寄回流程和退款时效规则。";
            case "REFUND_PROGRESS" -> "在回复中突出预计到账时间和查询入口。";
            case "LOGISTICS_QUERY" -> "联动物流异常订单，必要时自动创建工单。";
            case "COMPLAINT_TRANSFER" -> "优先进入人工工单队列，并记录处理承诺。";
            default -> "继续积累样本，观察是否成为高频问题。";
        };
    }

    private String ticketRisk(String status, int count) {
        if ("PENDING".equals(status)) {
            return count >= 3 ? "积压风险" : "待响应";
        }
        if ("PROCESSING".equals(status)) {
            return "处理中";
        }
        return "已闭环";
    }

    private String ticketAction(String status) {
        return switch (status) {
            case "PENDING" -> "优先分配客服并补充处理建议。";
            case "PROCESSING" -> "跟进处理时长，避免 SLA 超时。";
            case "RESOLVED", "CLOSED" -> "可沉淀为案例和知识库条目。";
            default -> "检查工单状态是否规范。";
        };
    }

    private String channelScenario(String channel) {
        return switch (channel) {
            case "WEB" -> "网页端用户咨询入口。";
            case "ADMIN_TEST" -> "管理员测试与答辩演示入口。";
            default -> "预留多渠道接入入口。";
        };
    }
}
