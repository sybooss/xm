package com.user.returnsassistant.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.user.returnsassistant.pojo.C2paDetectionResult;
import com.user.returnsassistant.service.C2paDetectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class C2paDetectionServiceImpl implements C2paDetectionService {
    private static final int MAX_OUTPUT_LENGTH = 6000;
    private static final List<String> OPENAI_KEYWORDS = List.of("openai", "chatgpt", "gpt-image", "dall-e", "dalle");
    private static final List<String> GOOGLE_KEYWORDS = List.of("google", "gemini", "imagen");
    private static final List<String> AI_SOURCE_KEYWORDS = List.of(
            "openai", "chatgpt", "gpt-image", "dall-e", "dalle", "google", "gemini", "imagen",
            "midjourney", "stable diffusion", "stablediffusion", "firefly", "adobe"
    );

    private final ObjectMapper objectMapper;

    @Value("${app.upload.root:tmp/uploads}")
    private String uploadRoot;
    @Value("${app.provenance.c2pa.enabled:true}")
    private boolean enabled;
    @Value("${app.provenance.c2pa.tool-path:c2patool}")
    private String toolPath;
    @Value("${app.provenance.c2pa.timeout-seconds:8}")
    private Integer timeoutSeconds;

    public C2paDetectionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public C2paDetectionResult detect(String fileUrl) {
        if (!enabled) {
            return C2paDetectionResult.skipped("C2PA 内容凭证检测已关闭，已使用视觉模型和本地规则兜底。");
        }
        if (!hasText(toolPath)) {
            return C2paDetectionResult.notConfigured("未配置 c2patool 路径，无法读取 C2PA 内容凭证。");
        }
        Path imagePath = resolveUploadPath(fileUrl);
        if (imagePath == null) {
            return C2paDetectionResult.skipped("当前图片不是本地上传文件，跳过 C2PA 内容凭证检测。");
        }
        if (!Files.isRegularFile(imagePath)) {
            return C2paDetectionResult.failed("未找到聊天图片原始文件，无法执行 C2PA 内容凭证检测。", null);
        }

        ToolOutput output = runTool(imagePath);
        if (output.notConfigured()) {
            return C2paDetectionResult.notConfigured("未找到 c2patool，请安装后设置 C2PATOOL_PATH；当前已使用视觉模型和本地规则兜底。");
        }
        String combined = trim(output.stdout() + "\n" + output.stderr(), MAX_OUTPUT_LENGTH);
        if (!output.completed()) {
            return C2paDetectionResult.failed("C2PA 内容凭证检测超时或执行失败，建议人工查看原始文件。", combined);
        }
        String normalized = normalize(combined);
        if (output.exitCode() != 0 && containsAny(normalized, "not found", "no claim", "no c2pa", "manifest not found", "could not find")) {
            return C2paDetectionResult.notFound("未发现 C2PA 内容凭证，不能据此证明图片来源。", combined);
        }
        if (output.exitCode() != 0 && !hasText(output.stdout())) {
            return C2paDetectionResult.failed("c2patool 返回错误，未能读取 C2PA 内容凭证。", combined);
        }

        String searchable = searchableText(output.stdout(), combined);
        Detection detection = classify(searchable);
        if (detection.detected()) {
            return C2paDetectionResult.detected(detection.provider(), detection.generator(),
                    "C2PA 内容凭证中出现 " + detection.provider() + sourceSuffix(detection.generator()) + " 来源信号，建议按疑似 AI 生成凭证人工复核。",
                    combined);
        }
        if (hasText(searchable)) {
            return C2paDetectionResult.notFound("图片存在可读取内容凭证，但未发现 OpenAI/GPT Image 等 AI 生成来源信号。", combined);
        }
        return C2paDetectionResult.notFound("未发现 C2PA 内容凭证，不能据此证明图片来源。", combined);
    }

    private Path resolveUploadPath(String fileUrl) {
        if (!hasText(fileUrl) || !fileUrl.startsWith("/uploads/")) {
            return null;
        }
        Path root = Path.of(uploadRoot).toAbsolutePath().normalize();
        String relative = fileUrl.substring("/uploads/".length()).replace('/', File.separatorChar);
        Path file = root.resolve(relative).normalize();
        return file.startsWith(root) ? file : null;
    }

    private ToolOutput runTool(Path imagePath) {
        List<List<String>> commands = List.of(
                List.of(toolPath, imagePath.toString(), "--json"),
                List.of(toolPath, imagePath.toString())
        );
        ToolOutput last = null;
        for (List<String> command : commands) {
            ToolOutput output = runCommand(command);
            if (output.notConfigured()) {
                return output;
            }
            last = output;
            if (output.completed() && output.exitCode() == 0 && hasText(output.stdout())) {
                return output;
            }
        }
        return last == null ? new ToolOutput(false, false, -1, "", "c2patool not executed") : last;
    }

    private ToolOutput runCommand(List<String> command) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            process = builder.start();
            boolean finished = process.waitFor(Math.max(1, safeInt(timeoutSeconds, 8)), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ToolOutput(false, false, -1, "", "c2patool timeout after " + safeInt(timeoutSeconds, 8) + " seconds");
            }
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ToolOutput(true, false, process.exitValue(), trim(stdout, MAX_OUTPUT_LENGTH), trim(stderr, MAX_OUTPUT_LENGTH));
        } catch (java.io.IOException e) {
            return new ToolOutput(false, true, -1, "", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ToolOutput(false, false, -1, "", "c2patool interrupted");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String searchableText(String stdout, String fallback) {
        if (!hasText(stdout)) {
            return normalize(fallback);
        }
        try {
            JsonNode root = objectMapper.readTree(stdout);
            List<String> values = new ArrayList<>();
            collectText(root, values);
            String joined = String.join(" ", values);
            return normalize(joined + " " + stdout);
        } catch (Exception ignored) {
            return normalize(stdout + " " + fallback);
        }
    }

    private void collectText(JsonNode node, List<String> values) {
        if (node == null || values.size() > 500) {
            return;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            values.add(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectText(item, values);
            }
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                values.add(field.getKey());
                collectText(field.getValue(), values);
            }
        }
    }

    private Detection classify(String searchable) {
        if (!hasText(searchable)) {
            return Detection.notDetected();
        }
        if (containsAny(searchable, OPENAI_KEYWORDS)) {
            return new Detection(true, "OpenAI", firstKeyword(searchable, OPENAI_KEYWORDS));
        }
        if (containsAny(searchable, GOOGLE_KEYWORDS)) {
            return new Detection(true, "Google", firstKeyword(searchable, GOOGLE_KEYWORDS));
        }
        if (containsAny(searchable, AI_SOURCE_KEYWORDS)) {
            return new Detection(true, "AI 平台", firstKeyword(searchable, AI_SOURCE_KEYWORDS));
        }
        return Detection.notDetected();
    }

    private String firstKeyword(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return "";
    }

    private String sourceSuffix(String generator) {
        return hasText(generator) ? " / " + generator : "";
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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

    private record ToolOutput(boolean completed, boolean notConfigured, int exitCode, String stdout, String stderr) {
    }

    private record Detection(boolean detected, String provider, String generator) {
        static Detection notDetected() {
            return new Detection(false, null, null);
        }
    }
}
