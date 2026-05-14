package com.user.returnsassistant.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class C2paDetectionResult {
    private String status;
    private String provider;
    private String generator;
    private String signal;
    private String rawSummary;

    public static C2paDetectionResult skipped(String signal) {
        return new C2paDetectionResult("SKIPPED", null, null, signal, null);
    }

    public static C2paDetectionResult notConfigured(String signal) {
        return new C2paDetectionResult("NOT_CONFIGURED", null, null, signal, null);
    }

    public static C2paDetectionResult notFound(String signal, String rawSummary) {
        return new C2paDetectionResult("NOT_FOUND", null, null, signal, rawSummary);
    }

    public static C2paDetectionResult failed(String signal, String rawSummary) {
        return new C2paDetectionResult("FAILED", null, null, signal, rawSummary);
    }

    public static C2paDetectionResult detected(String provider, String generator, String signal, String rawSummary) {
        return new C2paDetectionResult("DETECTED", provider, generator, signal, rawSummary);
    }
}
