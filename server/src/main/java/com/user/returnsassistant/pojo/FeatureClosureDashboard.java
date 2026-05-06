package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class FeatureClosureDashboard {
    private LocalDateTime generatedAt;
    private List<ClosureMetric> metrics = new ArrayList<>();
    private List<FeatureClosure> closures = new ArrayList<>();
    private List<DemoStep> demoSteps = new ArrayList<>();
    private List<ReferenceSource> references = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClosureMetric {
        private String label;
        private String value;
        private String detail;
        private String tone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureClosure {
        private Integer sequence;
        private String code;
        private String title;
        private String capability;
        private String status;
        private String signal;
        private String diagnosis;
        private String nextAction;
        private String route;
        private String endpoint;
        private String evidence;
        private String sourceInspiration;
        private Integer score;
        private String tone;
        private Boolean closedLoop;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemoStep {
        private Integer sequence;
        private String title;
        private String route;
        private String proof;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceSource {
        private String name;
        private String url;
        private String borrowedIdea;
        private String localLanding;
    }
}
