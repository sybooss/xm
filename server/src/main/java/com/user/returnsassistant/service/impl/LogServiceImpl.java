package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.mapper.AiCallLogMapper;
import com.user.returnsassistant.mapper.ProcessTraceMapper;
import com.user.returnsassistant.mapper.RetrievalLogMapper;
import com.user.returnsassistant.pojo.AiCallLog;
import com.user.returnsassistant.pojo.LogDiagnostics;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProcessTrace;
import com.user.returnsassistant.pojo.RetrievalLog;
import com.user.returnsassistant.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LogServiceImpl implements LogService {
    private static final int DIAGNOSTIC_SAMPLE_SIZE = 50;
    private static final int TREND_WINDOW_SIZE = 10;

    @Autowired
    private AiCallLogMapper aiCallLogMapper;
    @Autowired
    private RetrievalLogMapper retrievalLogMapper;
    @Autowired
    private ProcessTraceMapper processTraceMapper;

    @Override
    public PageResult<AiCallLog> pageAiLogs(Integer page, Integer pageSize, String status) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, size);
        Page<AiCallLog> result = (Page<AiCallLog>) aiCallLogMapper.page(status);
        return new PageResult<>(result.getTotal(), result.getResult());
    }

    @Override
    public PageResult<RetrievalLog> pageRetrievalLogs(Integer page, Integer pageSize, String keyword) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, size);
        Page<RetrievalLog> result = (Page<RetrievalLog>) retrievalLogMapper.page(keyword);
        return new PageResult<>(result.getTotal(), result.getResult());
    }

    @Override
    public LogDiagnostics getDiagnostics() {
        PageHelper.startPage(1, DIAGNOSTIC_SAMPLE_SIZE);
        List<AiCallLog> aiLogs = aiCallLogMapper.page(null);
        PageHelper.startPage(1, DIAGNOSTIC_SAMPLE_SIZE);
        List<RetrievalLog> retrievalLogs = retrievalLogMapper.page(null);
        List<ProcessTrace> traces = processTraceMapper.listRecent(100);

        LogDiagnostics diagnostics = new LogDiagnostics();
        diagnostics.setGeneratedAt(LocalDateTime.now());
        diagnostics.setAi(buildAiHealth(aiLogs));
        diagnostics.setRetrieval(buildRetrievalHealth(retrievalLogs));
        diagnostics.setTrace(buildTraceHealth(traces));
        diagnostics.setRiskSignals(buildRiskSignals(diagnostics));
        diagnostics.setActionItems(buildActionItems(diagnostics));
        return diagnostics;
    }

    private LogDiagnostics.AiHealth buildAiHealth(List<AiCallLog> logs) {
        LogDiagnostics.AiHealth health = new LogDiagnostics.AiHealth();
        int total = logs.size();
        int success = countStatus(logs, "SUCCESS");
        int failed = countStatus(logs, "FAILED");
        int skipped = countStatus(logs, "SKIPPED");

        health.setSampleSize(total);
        health.setSuccessCount(success);
        health.setFailedCount(failed);
        health.setSkippedCount(skipped);
        health.setTotalTokens(logs.stream()
                .mapToInt(item -> safeInt(item.getPromptTokens()) + safeInt(item.getCompletionTokens()))
                .sum());
        health.setAverageLatencyMs(averageLatency(logs));
        health.setSuccessRateLabel(total == 0 ? "-" : Math.round(success * 100.0 / total) + "%");
        health.setAverageLatencyLabel(health.getAverageLatencyMs() == 0 ? "-" : health.getAverageLatencyMs() + " ms");
        health.setPrimaryModel(primaryModel(logs));
        applyTrend(health, logs);
        applyHealthLevel(health);
        applyFailureEntry(health, logs);
        return health;
    }

    private int countStatus(List<AiCallLog> logs, String status) {
        return (int) logs.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int averageLatency(List<AiCallLog> logs) {
        List<Integer> values = logs.stream()
                .map(AiCallLog::getLatencyMs)
                .filter(Objects::nonNull)
                .filter(value -> value > 0)
                .toList();
        if (values.isEmpty()) {
            return 0;
        }
        return (int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private String primaryModel(List<AiCallLog> logs) {
        return logs.stream()
                .filter(item -> hasText(item.getProvider()) || hasText(item.getModelName()))
                .findFirst()
                .map(item -> {
                    String provider = hasText(item.getProvider()) ? item.getProvider() : null;
                    String modelName = hasText(item.getModelName()) ? item.getModelName() : null;
                    if (provider != null && modelName != null) {
                        return provider + " / " + modelName;
                    }
                    return provider != null ? provider : modelName;
                })
                .orElse("-");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void applyTrend(LogDiagnostics.AiHealth health, List<AiCallLog> logs) {
        Double recent = successRate(logs.subList(0, Math.min(TREND_WINDOW_SIZE, logs.size())));
        Double previous = logs.size() <= TREND_WINDOW_SIZE
                ? null
                : successRate(logs.subList(TREND_WINDOW_SIZE, Math.min(TREND_WINDOW_SIZE * 2, logs.size())));
        health.setRecentSuccessRate(recent);
        health.setPreviousSuccessRate(previous);

        if (recent == null) {
            health.setTrendLabel("等待样本");
            health.setTrendDetail("还没有 AI 调用日志，先完成一次聊天或 AI 测试。");
            return;
        }
        String current = Math.round(recent * 100) + "%";
        if (previous == null) {
            health.setTrendLabel("样本积累中");
            health.setTrendDetail("最近 " + Math.min(TREND_WINDOW_SIZE, logs.size()) + " 条样本成功率 " + current + "。");
            return;
        }
        double delta = recent - previous;
        if (delta >= 0.1) {
            health.setTrendLabel("成功率上升");
        } else if (delta <= -0.1) {
            health.setTrendLabel("成功率下降");
        } else {
            health.setTrendLabel("趋势平稳");
        }
        health.setTrendDetail("最近窗口 " + current + "，上一窗口 " + Math.round(previous * 100) + "%，用于判断模型链路是否波动。");
    }

    private Double successRate(List<AiCallLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        return countStatus(logs, "SUCCESS") * 1.0 / logs.size();
    }

    private void applyHealthLevel(LogDiagnostics.AiHealth health) {
        int total = health.getSampleSize();
        if (total == 0) {
            return;
        }
        double failedRate = health.getFailedCount() * 1.0 / total;
        if (failedRate >= 0.3) {
            health.setHealthLevel("需要关注");
            health.setHealthTone("danger");
            health.setHealthRationale("最近样本中有 " + health.getFailedCount() + " 次失败，应优先检查模型网关和密钥配置。");
        } else if (health.getFailedCount() > 0) {
            health.setHealthLevel("可用但有波动");
            health.setHealthTone("warning");
            health.setHealthRationale("存在 " + health.getFailedCount() + " 次失败，但主链路仍保留本地兜底。");
        } else if (health.getSkippedCount() == total) {
            health.setHealthLevel("兜底稳定");
            health.setHealthTone("success");
            health.setHealthRationale("当前全部走本地兜底，适合无模型环境稳定演示。");
        } else if (health.getRecentSuccessRate() != null && health.getRecentSuccessRate() >= 0.8) {
            health.setHealthLevel("运行稳定");
            health.setHealthTone("success");
            health.setHealthRationale("最近调用成功率和检索证据都可用于支撑系统稳定性说明。");
        } else {
            health.setHealthLevel("样本观察中");
            health.setHealthTone("warning");
            health.setHealthRationale("样本量或成功率还不足以判断长期稳定性，建议补充多轮演示样本。");
        }
    }

    private void applyFailureEntry(LogDiagnostics.AiHealth health, List<AiCallLog> logs) {
        logs.stream()
                .filter(item -> "FAILED".equals(item.getStatus()))
                .findFirst()
                .ifPresent(item -> {
                    health.setFailureEntryLabel(item.getSessionId() == null ? "日志 " + item.getId() : "会话 " + item.getSessionId());
                    health.setFailureEntryDetail(hasText(item.getErrorMessage()) ? item.getErrorMessage() : "失败日志缺少错误摘要，建议检查后端运行日志。");
                });
    }

    private LogDiagnostics.RetrievalHealth buildRetrievalHealth(List<RetrievalLog> logs) {
        LogDiagnostics.RetrievalHealth health = new LogDiagnostics.RetrievalHealth();
        health.setSampleSize(logs.size());
        health.setUniqueDocCount((int) logs.stream()
                .map(RetrievalLog::getDocTitleSnapshot)
                .filter(this::hasText)
                .distinct()
                .count());
        health.setAverageScoreLabel(averageScoreLabel(logs));
        health.setTopDocs(topDocs(logs));
        return health;
    }

    private String averageScoreLabel(List<RetrievalLog> logs) {
        List<BigDecimal> values = logs.stream()
                .map(RetrievalLog::getScore)
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return "-";
        }
        double average = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        return String.format("%.2f", average);
    }

    private List<LogDiagnostics.TopDoc> topDocs(List<RetrievalLog> logs) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        logs.forEach(item -> {
            String title = hasText(item.getDocTitleSnapshot()) ? item.getDocTitleSnapshot() : "未命名文档";
            counts.put(title, counts.getOrDefault(title, 0) + 1);
        });
        return counts.entrySet().stream()
                .map(entry -> new LogDiagnostics.TopDoc(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> b.getCount().compareTo(a.getCount()))
                .limit(5)
                .toList();
    }

    private LogDiagnostics.TraceHealth buildTraceHealth(List<ProcessTrace> traces) {
        LogDiagnostics.TraceHealth health = new LogDiagnostics.TraceHealth();
        if (traces.isEmpty()) {
            return health;
        }
        Long latestSessionId = traces.get(0).getSessionId();
        List<ProcessTrace> latestSessionTraces = traces.stream()
                .filter(item -> Objects.equals(item.getSessionId(), latestSessionId))
                .toList();
        int success = (int) latestSessionTraces.stream()
                .filter(item -> "SUCCESS".equals(item.getStepStatus()))
                .count();
        health.setLatestSessionId(latestSessionId);
        health.setLatestStepCount(latestSessionTraces.size());
        health.setLatestSuccessCount(success);
        health.setLatestProgressLabel(success + "/" + latestSessionTraces.size());
        return health;
    }

    private List<LogDiagnostics.RiskSignal> buildRiskSignals(LogDiagnostics diagnostics) {
        LogDiagnostics.AiHealth ai = diagnostics.getAi();
        LogDiagnostics.RetrievalHealth retrieval = diagnostics.getRetrieval();
        List<LogDiagnostics.RiskSignal> signals = new ArrayList<>();

        if (ai.getSampleSize() == 0) {
            signals.add(new LogDiagnostics.RiskSignal("样本不足", "还没有 AI 调用日志，无法判断模型链路稳定性。", "info"));
        }
        if (ai.getFailedCount() > 0) {
            String tone = ai.getFailedCount() * 1.0 / Math.max(ai.getSampleSize(), 1) >= 0.3 ? "danger" : "warning";
            signals.add(new LogDiagnostics.RiskSignal("模型调用失败", "最近 " + ai.getSampleSize() + " 条样本中有 " + ai.getFailedCount() + " 次失败，需要关注网关、密钥或模型名。", tone));
        }
        if (ai.getSampleSize() > 0 && ai.getSkippedCount().equals(ai.getSampleSize())) {
            signals.add(new LogDiagnostics.RiskSignal("全部本地兜底", "系统可稳定演示，但当前样本不能证明真实模型链路已经打通。", "warning"));
        }
        if (ai.getAverageLatencyMs() > 6000) {
            signals.add(new LogDiagnostics.RiskSignal("响应偏慢", "平均耗时 " + ai.getAverageLatencyMs() + " ms，演示时建议先确认 sub2api 或模型服务状态。", "warning"));
        }
        if (retrieval.getSampleSize() == 0) {
            signals.add(new LogDiagnostics.RiskSignal("缺少 RAG 证据", "当前没有知识检索日志，答辩时难以展示回答依据链路。", "info"));
        } else if (retrieval.getSampleSize() >= 5 && retrieval.getUniqueDocCount() <= 1) {
            signals.add(new LogDiagnostics.RiskSignal("命中文档集中", "最近检索主要集中在单一文档，可补充关键词或意图覆盖来提升召回解释力。", "warning"));
        }
        if (signals.isEmpty()) {
            signals.add(new LogDiagnostics.RiskSignal("暂无明显风险", "最近样本中模型调用、知识命中和日志证据处于可演示状态。", "success"));
        }
        return signals;
    }

    private List<String> buildActionItems(LogDiagnostics diagnostics) {
        LogDiagnostics.AiHealth ai = diagnostics.getAi();
        LogDiagnostics.RetrievalHealth retrieval = diagnostics.getRetrieval();
        List<String> items = new ArrayList<>();
        if (ai.getSampleSize() == 0) {
            items.add("先在咨询工作台发送一轮售后问题，生成 AI 调用、检索和处理轨迹样本。");
        }
        if (ai.getFailedCount() > 0) {
            items.add("优先检查 OPENAI_BASE_URL、OPENAI_API_KEY、模型名和 sub2api 健康状态。");
        }
        if (ai.getSampleSize() > 0 && ai.getSkippedCount().equals(ai.getSampleSize())) {
            items.add("如果要展示真实模型能力，开启 AI 配置后重新执行 AI 测试和聊天烟测。");
        }
        if (retrieval.getSampleSize() == 0 || retrieval.getUniqueDocCount() <= 1) {
            items.add("用知识库调试面板检索退货、退款、物流、投诉等问题，确认多类规则都能留下命中日志。");
        }
        items.add("演示时先展示健康趋势，再切到 AI 调用日志、知识检索日志和处理轨迹三类原始证据。");
        return items.stream().limit(4).toList();
    }
}
