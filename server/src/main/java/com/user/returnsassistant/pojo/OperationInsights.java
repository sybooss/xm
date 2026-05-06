package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OperationInsights {
    private LocalDateTime generatedAt;
    private List<InsightMetric> metrics = new ArrayList<>();
    private List<FeatureCard> newFeatures = new ArrayList<>();
    private List<IntentInsight> intentInsights = new ArrayList<>();
    private List<TicketInsight> ticketInsights = new ArrayList<>();
    private List<ChannelInsight> channelInsights = new ArrayList<>();
    private List<KnowledgeInsight> knowledgeInsights = new ArrayList<>();
    private List<OrderRiskInsight> orderRiskInsights = new ArrayList<>();
    private List<AiInsight> aiInsights = new ArrayList<>();
    private List<ActionItem> actionItems = new ArrayList<>();
    private List<VersionMilestone> versionMilestones = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightMetric {
        private String label;
        private String value;
        private String detail;
        private String tone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureCard {
        private Integer sequence;
        private String title;
        private String category;
        private String status;
        private String detail;
        private String route;
        private String endpoint;
        private String evidence;
        private String validation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentInsight {
        private String intentCode;
        private String intentName;
        private Integer count;
        private String shareLabel;
        private String suggestion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInsight {
        private String status;
        private Integer count;
        private String riskLabel;
        private String suggestedAction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelInsight {
        private String channel;
        private Integer count;
        private String shareLabel;
        private String scenario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeInsight {
        private String title;
        private Integer hitCount;
        private BigDecimal averageScore;
        private String scoreLabel;
        private String action;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderRiskInsight {
        private String orderNo;
        private String productName;
        private String riskType;
        private String riskReason;
        private String suggestedAction;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiInsight {
        private String label;
        private String value;
        private String detail;
        private String tone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String title;
        private String owner;
        private String priority;
        private String detail;
        private String route;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionMilestone {
        private String version;
        private String date;
        private String detail;
        private Boolean done;
    }
}
