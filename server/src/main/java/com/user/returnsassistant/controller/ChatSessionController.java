package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.ChatMessageRequest;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.ChatSessionSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/chat-sessions")
public class ChatSessionController {
    @Autowired
    private ChatService chatService;

    @GetMapping
    public Result page(ChatSessionSearch search) {
        return Result.success(chatService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody ChatSession session) {
        return Result.success(chatService.save(session));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(chatService.getDetail(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody ChatSession session) {
        chatService.update(id, session);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        chatService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result listMessages(@PathVariable Long id) {
        return Result.success(chatService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Result sendMessage(@PathVariable Long id, @RequestBody ChatMessageRequest request) {
        return Result.success(chatService.sendMessage(id, request));
    }

    @PostMapping(value = "/{id}/message-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@PathVariable Long id, @RequestBody ChatMessageRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "progress", Map.of("stage", "RECEIVED", "message", "消息已发送，正在解析上下文"));
                sendEvent(emitter, "progress", Map.of("stage", "ANALYZING", "message", "正在识别意图、读取订单并检索知识库"));
                Map<String, Object> result = chatService.sendMessage(id, request);
                sendEvent(emitter, "progress", Map.of("stage", "GENERATED", "message", "模型回复已生成，正在写入会话记录"));
                sendEvent(emitter, "final", result);
                emitter.complete();
            } catch (Exception e) {
                try {
                    sendEvent(emitter, "error", Map.of("message", e.getMessage() == null ? "消息流处理失败" : e.getMessage()));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @GetMapping("/{id}/process-traces")
    public Result listTraces(@PathVariable Long id) {
        return Result.success(chatService.listTraces(id));
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data));
    }
}
