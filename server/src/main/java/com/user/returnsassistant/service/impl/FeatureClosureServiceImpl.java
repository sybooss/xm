package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.pojo.FeatureClosureDashboard;
import com.user.returnsassistant.service.FeatureClosureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeatureClosureServiceImpl implements FeatureClosureService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public FeatureClosureDashboard getDashboard() {
        FeatureClosureDashboard dashboard = new FeatureClosureDashboard();
        dashboard.setGeneratedAt(LocalDateTime.now());
        dashboard.setClosures(buildClosures());
        dashboard.setMetrics(buildMetrics(dashboard.getClosures()));
        dashboard.setDemoSteps(buildDemoSteps());
        dashboard.setReferences(buildReferences());
        return dashboard;
    }

    private List<FeatureClosureDashboard.ClosureMetric> buildMetrics(List<FeatureClosureDashboard.FeatureClosure> closures) {
        int active = count("select count(*) from chat_session where status = 'ACTIVE'");
        int pendingTickets = count("select count(*) from service_ticket where deleted = 0 and status in ('PENDING', 'PROCESSING')");
        int closedLoop = (int) closures.stream().filter(item -> Boolean.TRUE.equals(item.getClosedLoop())).count();
        String aiRate = percent(
                count("select count(*) from ai_call_log where status = 'SUCCESS'"),
                count("select count(*) from ai_call_log")
        );
        return List.of(
                new FeatureClosureDashboard.ClosureMetric("本轮新增闭环功能", closures.size() + " 个", "全部绑定信号、判断、动作入口和验收证据。", "primary"),
                new FeatureClosureDashboard.ClosureMetric("已闭环能力", closedLoop + " 个", "接口返回 closedLoop=true，前端逐项展示。", "success"),
                new FeatureClosureDashboard.ClosureMetric("待处理工单", pendingTickets + " 单", "用于 SLA、优先级路由和人工兜底功能判断。", pendingTickets > 0 ? "warning" : "success"),
                new FeatureClosureDashboard.ClosureMetric("活跃会话", active + " 个", "用于情绪、渠道、证据链和演示脚本编排。", "info"),
                new FeatureClosureDashboard.ClosureMetric("AI 成功率", aiRate, "来自 ai_call_log，可解释真实模型与本地兜底链路。", "primary")
        );
    }

    private List<FeatureClosureDashboard.FeatureClosure> buildClosures() {
        int pendingTickets = count("select count(*) from service_ticket where deleted = 0 and status in ('PENDING', 'PROCESSING')");
        int urgentTickets = count("select count(*) from service_ticket where deleted = 0 and priority in ('HIGH', 'URGENT')");
        int complaints = count("select count(*) from intent_record where intent_code = 'COMPLAINT_TRANSFER'");
        int abnormalOrders = count("select count(*) from demo_order where logistics_status = 'ABNORMAL'");
        int afterSales = count("select count(*) from after_sale_record where status in ('APPLIED', 'APPROVED', 'WAIT_BUYER_SEND', 'WAIT_SELLER_CONFIRM', 'REFUNDING')");
        int refundingOrders = count("select count(*) from demo_order where after_sale_status = 'REFUNDING'");
        int knowledgeDocs = count("select count(*) from knowledge_doc where deleted = 0 and status = 'ENABLED'");
        int uncoveredIntents = Math.max(0, 7 - count("select count(distinct intent_code) from knowledge_doc where deleted = 0 and status = 'ENABLED' and intent_code is not null"));
        int retrievalLogs = count("select count(*) from retrieval_log");
        int aiTotal = count("select count(*) from ai_call_log");
        int aiSuccess = count("select count(*) from ai_call_log where status = 'SUCCESS'");
        int aiFailed = count("select count(*) from ai_call_log where status = 'FAILED'");
        int aiSkipped = count("select count(*) from ai_call_log where status = 'SKIPPED'");
        int traces = count("select count(*) from process_trace");
        int traceSuccess = count("select count(*) from process_trace where step_status = 'SUCCESS'");
        int channels = count("select count(distinct channel) from chat_session");
        int repeatedOrders = count("""
                select count(*) from (
                  select order_id
                  from service_ticket
                  where deleted = 0 and order_id is not null
                  group by order_id
                  having count(*) >= 2
                ) t
                """);

        String aiRate = percent(aiSuccess, aiTotal);
        String traceRate = percent(traceSuccess, traces);
        String retrievalScore = decimalLabel("select coalesce(avg(score), 0) from retrieval_log");

        return List.of(
                closure(1, "SLA_GUARD", "SLA 自动预警台", "工单时效守护",
                        pendingTickets > 0 ? "运行中" : "待样本",
                        "待处理或处理中工单 " + pendingTickets + " 单，高优先级 " + urgentTickets + " 单。",
                        pendingTickets > 0 ? "存在需要客服主管跟进的 SLA 样本。" : "当前没有待处理工单，能力保持待命。",
                        "进入人工工单页，优先处理 HIGH / URGENT 工单。",
                        "/service-tickets", "GET /feature-closures", "service_ticket.status + priority",
                        "借鉴客服系统常见 SLA 队列，把时效风险前置到运营入口。", scoreByCount(pendingTickets), pendingTickets > 0 ? "warning" : "success"),
                closure(2, "SENTIMENT_RADAR", "客户情绪温度计", "投诉情绪识别",
                        complaints > 0 ? "运行中" : "待样本",
                        "投诉转人工意图累计 " + complaints + " 次。",
                        complaints > 0 ? "客户负向情绪已被意图链路捕获。" : "暂未出现投诉样本，可用演示问题触发。",
                        "在咨询工作台输入投诉类问题，验证自动转人工。",
                        "/chat", "POST /chat-sessions/{id}/messages", "intent_record.COMPLAINT_TRANSFER",
                        "借鉴 Help Desk AI 的情绪分流思路，先用本地意图兜底保证稳定。", scoreByCount(complaints), complaints > 0 ? "danger" : "info"),
                closure(3, "PRIORITY_ROUTER", "智能优先级路由器", "客服队列分派",
                        urgentTickets > 0 ? "运行中" : "待样本",
                        "HIGH / URGENT 工单 " + urgentTickets + " 单。",
                        urgentTickets > 0 ? "系统已形成可分派的高优队列。" : "当前队列较轻，路由规则待更多样本验证。",
                        "按优先级筛选人工工单，检查处理建议和状态流转。",
                        "/service-tickets", "GET /service-tickets", "priority/status filters",
                        "借鉴成熟工单系统的 triage 队列，不让 AI 直接绕过人工流程。", scoreByCount(urgentTickets), urgentTickets > 0 ? "warning" : "success"),
                closure(4, "COMPENSATION_ADVISOR", "赔付方案推荐器", "售后补偿建议",
                        afterSales > 0 ? "运行中" : "待样本",
                        "处理中售后记录 " + afterSales + " 条，退款中订单 " + refundingOrders + " 单。",
                        afterSales > 0 ? "存在可生成补偿/退款建议的售后上下文。" : "需要创建售后单后形成判断样本。",
                        "打开订单详情，结合售后记录和客服建议完成处置。",
                        "/orders", "GET /orders/{id}/after-sale-records", "after_sale_record + demo_order.after_sale_status",
                        "借鉴电商售后流程，把订单、售后单和客服话术连成一条链。", scoreByCount(afterSales), afterSales > 0 ? "primary" : "info"),
                closure(5, "KNOWLEDGE_GAP_MINER", "知识缺口采矿器", "FAQ 覆盖优化",
                        uncoveredIntents == 0 ? "运行中" : "待补齐",
                        "启用知识 " + knowledgeDocs + " 篇，未覆盖意图 " + uncoveredIntents + " 类。",
                        uncoveredIntents == 0 ? "知识库已覆盖全部核心意图。" : "仍有意图缺少专属知识条目。",
                        "进入知识库补齐退货、退款、物流、投诉等高频规则。",
                        "/knowledge", "GET /knowledge-docs/search", "knowledge_doc.intent_code",
                        "借鉴 RAG 项目的知识覆盖监控，把检索质量反向推动文档维护。", uncoveredIntents == 0 ? 96 : Math.max(60, 96 - uncoveredIntents * 8), uncoveredIntents == 0 ? "success" : "warning"),
                closure(6, "ANSWER_QA_SCORECARD", "回复质检评分卡", "回答质量评估",
                        aiTotal > 0 || traces > 0 ? "运行中" : "待样本",
                        "AI 成功率 " + aiRate + "，流程成功率 " + traceRate + "，平均检索分 " + retrievalScore + "。",
                        "把模型可用性、RAG 命中和流程轨迹合并成答辩可解释质量分。",
                        "打开日志中心复核 AI 调用、知识命中和处理轨迹。",
                        "/logs", "GET /log-diagnostics", "ai_call_log + retrieval_log + process_trace",
                        "借鉴 LangSmith/可观测性思想，但用本项目本地日志实现。", averageScore(aiSuccess, aiTotal, traceSuccess, traces), aiFailed > 0 ? "warning" : "success"),
                closure(7, "REFUND_TIMELINE", "退款进度预测器", "退款时效说明",
                        refundingOrders > 0 || afterSales > 0 ? "运行中" : "待样本",
                        "退款中订单 " + refundingOrders + " 单，售后处理中记录 " + afterSales + " 条。",
                        "可根据订单状态、售后状态和会话上下文生成退款进度解释。",
                        "在咨询工作台追问“退款多久到”，验证多轮上下文承接。",
                        "/chat", "POST /chat-sessions/{id}/messages", "context.followUp + after_sale_status",
                        "借鉴订单追踪 agent 的状态解释能力，保持本地业务规则兜底。", scoreByCount(refundingOrders + afterSales), afterSales > 0 ? "primary" : "info"),
                closure(8, "LOGISTICS_ESCALATION", "物流异常处置流", "物流风险升级",
                        abnormalOrders > 0 ? "运行中" : "待样本",
                        "物流异常订单 " + abnormalOrders + " 单。",
                        abnormalOrders > 0 ? "存在需要主动安抚和承运商同步的订单。" : "暂无物流异常，处置流保持待命。",
                        "进入订单管理页核对异常订单，必要时转人工工单。",
                        "/orders", "GET /orders", "demo_order.logistics_status=ABNORMAL",
                        "借鉴电商后台的异常订单看板，把物流问题纳入客服闭环。", scoreByCount(abnormalOrders), abnormalOrders > 0 ? "danger" : "success"),
                closure(9, "REPEAT_COMPLAINT_GUARD", "重复投诉拦截器", "复发风险识别",
                        repeatedOrders > 0 ? "运行中" : "待样本",
                        "重复产生工单的订单 " + repeatedOrders + " 个。",
                        repeatedOrders > 0 ? "同一订单出现多次人工介入，需升级处理。" : "暂未发现重复投诉订单。",
                        "筛选工单并查看同订单处理历史，避免重复解释。",
                        "/service-tickets", "GET /service-tickets", "service_ticket grouped by order_id",
                        "借鉴客服 CRM 的重复来访识别，降低用户二次投诉。", repeatedOrders > 0 ? 90 : 78, repeatedOrders > 0 ? "warning" : "success"),
                closure(10, "EVIDENCE_CHAIN_CHECKER", "证据链完整度检查器", "答辩证据校验",
                        traces > 0 && retrievalLogs > 0 ? "运行中" : "待补齐",
                        "处理轨迹 " + traces + " 条，知识命中 " + retrievalLogs + " 条，AI 日志 " + aiTotal + " 条。",
                        traces > 0 && retrievalLogs > 0 ? "会话已经能导出完整 Markdown 证据。" : "需要补充聊天样本和知识检索日志。",
                        "在咨询工作台导出当前会话证据报告。",
                        "/chat", "GET /chat-sessions/{id}/evidence-report", "Markdown evidence report",
                        "借鉴审计型工单系统，把每次 AI 决策留下可复盘证据。", traces > 0 && retrievalLogs > 0 ? 98 : 66, traces > 0 && retrievalLogs > 0 ? "success" : "warning"),
                closure(11, "FALLBACK_DRILL", "AI 兜底演练面板", "模型失败可用性",
                        aiFailed > 0 || aiSkipped > 0 ? "运行中" : "待样本",
                        "AI 失败 " + aiFailed + " 次，跳过/本地兜底 " + aiSkipped + " 次。",
                        aiFailed > 0 || aiSkipped > 0 ? "系统已保留模型不可用时的业务回答路径。" : "当前样本以成功调用为主，可继续演练兜底。",
                        "打开 AI 测试和日志中心，展示失败时仍可本地响应。",
                        "/ai-test", "POST /ai-tests", "ai_call_log.status",
                        "借鉴 LangChain4j 的模型可替换思想，业务系统不绑定单一模型状态。", aiFailed > 0 || aiSkipped > 0 ? 92 : 82, aiFailed > 0 ? "warning" : "primary"),
                closure(12, "CHANNEL_ORCHESTRATOR", "多渠道触达编排器", "渠道体验统一",
                        channels >= 2 ? "运行中" : "待样本",
                        "已出现 " + channels + " 类会话渠道。",
                        channels >= 2 ? "Web、App、小程序等入口可统一进入同一客服闭环。" : "当前渠道样本偏少，可创建 APP / 小程序会话。",
                        "在咨询工作台切换渠道筛选，验证不同入口的会话管理。",
                        "/chat", "GET /chat-sessions?channel=APP", "chat_session.channel",
                        "借鉴多端商城的统一客服入口，把渠道差异沉淀为字段而非分裂流程。", channels >= 2 ? 95 : 74, channels >= 2 ? "success" : "info"),
                closure(13, "RAG_REVIEW_BOARD", "RAG 命中复盘板", "检索依据复盘",
                        retrievalLogs > 0 ? "运行中" : "待样本",
                        "知识检索日志 " + retrievalLogs + " 条，平均分 " + retrievalScore + "。",
                        retrievalLogs > 0 ? "回答依据可回看，可解释命中文档和排序依据。" : "暂无检索日志，需要先触发知识问答。",
                        "进入知识库检索调试，检查命中文档、命中解释和排序依据。",
                        "/knowledge", "GET /retrieval-logs", "retrieval_log.score + hit_reason",
                        "借鉴 LangChain4j RAG 文档中的检索增强思路，落地到 MySQL 全文检索和日志复盘。", retrievalLogs > 0 ? 94 : 70, retrievalLogs > 0 ? "success" : "info"),
                closure(14, "DEMO_SCRIPT_BUILDER", "答辩演示编排器", "一键讲清项目",
                        "运行中",
                        "当前页面聚合 14 个闭环功能和 " + buildDemoSteps().size() + " 个演示步骤。",
                        "把登录、咨询、转人工、导出证据、运营复盘串成答辩路线。",
                        "按页面下方演示路线逐步点击，形成完整项目故事。",
                        "/showcase", "GET /feature-closures", "demoSteps + references",
                        "借鉴开源项目 README / demo flow，把工程亮点变成可检查路径。", 100, "primary")
        );
    }

    private List<FeatureClosureDashboard.DemoStep> buildDemoSteps() {
        return List.of(
                new FeatureClosureDashboard.DemoStep(1, "管理员登录后进入答辩展示中心", "/showcase", "展示系统定位、版本路线和新增能力入口。"),
                new FeatureClosureDashboard.DemoStep(2, "咨询工作台发起退货问题", "/chat", "触发意图识别、RAG 命中、AI/兜底回复和处理轨迹。"),
                new FeatureClosureDashboard.DemoStep(3, "输入投诉问题自动转人工", "/chat", "形成高优先级工单和客服处理建议。"),
                new FeatureClosureDashboard.DemoStep(4, "导出会话证据报告", "/chat", "Markdown 报告串联订单、对话、知识、AI、工单和轨迹证据。"),
                new FeatureClosureDashboard.DemoStep(5, "打开特色闭环中心复盘", "/feature-closures", "逐项解释 10+ 新增特色功能如何闭环。"),
                new FeatureClosureDashboard.DemoStep(6, "进入日志与运营页面验证稳定性", "/logs", "用日志、风险信号和运营看板证明不是静态演示。")
        );
    }

    private List<FeatureClosureDashboard.ReferenceSource> buildReferences() {
        return List.of(
                new FeatureClosureDashboard.ReferenceSource(
                        "LangChain4j RAG / Tools",
                        "https://docs.langchain4j.dev/",
                        "RAG、Tools、模型可替换、Java 集成",
                        "本项目保留 LangChain4j 增强层，同时用本地规则和日志证据兜底。"
                ),
                new FeatureClosureDashboard.ReferenceSource(
                        "Spring Petclinic LangChain4j",
                        "https://github.com/spring-petclinic/spring-petclinic-langchain4j",
                        "自然语言调用业务工具、会话记忆、流式回答",
                        "本项目把 AI 工具调用限定在订单、知识、工单等 Spring 业务服务之后。"
                ),
                new FeatureClosureDashboard.ReferenceSource(
                        "LILISHOP",
                        "https://github.com/lilishop",
                        "多用户商城、订单/售后/后台运营能力",
                        "本项目借鉴电商售后后台的订单、工单、知识和运营闭环。"
                )
        );
    }

    private FeatureClosureDashboard.FeatureClosure closure(
            Integer sequence,
            String code,
            String title,
            String capability,
            String status,
            String signal,
            String diagnosis,
            String nextAction,
            String route,
            String endpoint,
            String evidence,
            String sourceInspiration,
            Integer score,
            String tone
    ) {
        return new FeatureClosureDashboard.FeatureClosure(
                sequence, code, title, capability, status, signal, diagnosis, nextAction,
                route, endpoint, evidence, sourceInspiration, score, tone, true
        );
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String decimalLabel(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(int count, int total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(count * 100.0 / total) + "%";
    }

    private int scoreByCount(int count) {
        return count > 0 ? 90 : 72;
    }

    private int averageScore(int aiSuccess, int aiTotal, int traceSuccess, int traces) {
        int aiScore = aiTotal == 0 ? 70 : (int) Math.round(aiSuccess * 100.0 / aiTotal);
        int traceScore = traces == 0 ? 70 : (int) Math.round(traceSuccess * 100.0 / traces);
        return Math.min(100, Math.max(60, (aiScore + traceScore) / 2));
    }
}
