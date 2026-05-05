package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LogDiagnostics {
    private LocalDateTime generatedAt;
    private AiHealth ai = new AiHealth();
    private RetrievalHealth retrieval = new RetrievalHealth();
    private TraceHealth trace = new TraceHealth();
    private List<RiskSignal> riskSignals = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();

    @Data
    public static class AiHealth {
        private Integer sampleSize = 0;
        private Integer successCount = 0;
        private Integer failedCount = 0;
        private Integer skippedCount = 0;
        private Integer averageLatencyMs = 0;
        private Integer totalTokens = 0;
        private String successRateLabel = "-";
        private String averageLatencyLabel = "-";
        private String primaryModel = "-";
        private String trendLabel = "等待样本";
        private String trendDetail = "还没有 AI 调用日志，先完成一次聊天或 AI 测试。";
        private String healthLevel = "等待样本";
        private String healthTone = "success";
        private String healthRationale = "日志页会在真实调用后自动形成健康判断。";
        private String failureEntryLabel = "暂无失败";
        private String failureEntryDetail = "最近样本没有失败记录，可继续查看原始日志作为佐证。";
        private Double recentSuccessRate;
        private Double previousSuccessRate;
    }

    @Data
    public static class RetrievalHealth {
        private Integer sampleSize = 0;
        private Integer uniqueDocCount = 0;
        private String averageScoreLabel = "-";
        private List<TopDoc> topDocs = new ArrayList<>();
    }

    @Data
    public static class TraceHealth {
        private Long latestSessionId;
        private Integer latestStepCount = 0;
        private Integer latestSuccessCount = 0;
        private String latestProgressLabel = "-";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDoc {
        private String title;
        private Integer count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskSignal {
        private String title;
        private String detail;
        private String tone;
    }
}
