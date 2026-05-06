package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.pojo.ChatMessageRequest;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.ChatSessionSearch;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.ChatService;
import com.user.returnsassistant.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/chat-sessions")
public class ChatSessionController {
    @Autowired
    private ChatService chatService;
    @Autowired
    private AuthService authService;
    @Autowired
    private OrderService orderService;

    @GetMapping
    public Result page(ChatSessionSearch search, HttpServletRequest request) {
        UserAccount user = requireRequestUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            search.setUserId(user.getId());
        }
        return Result.success(chatService.page(search));
    }

    @PostMapping
    public Result save(@RequestBody ChatSession session, HttpServletRequest request) {
        UserAccount user = requireRequestUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            session.setUserId(user.getId());
        } else if (session.getUserId() == null) {
            session.setUserId(user.getId());
        }
        ensureOrderAccess(session.getOrderNo(), user);
        return Result.success(chatService.save(session));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id, HttpServletRequest request) {
        ensureSessionAccess(id, request);
        return Result.success(chatService.getDetail(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody ChatSession session) {
        chatService.update(id, session);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id, HttpServletRequest request) {
        ensureSessionAccess(id, request);
        chatService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/messages")
    public Result listMessages(@PathVariable Long id, HttpServletRequest request) {
        ensureSessionAccess(id, request);
        return Result.success(chatService.listMessages(id));
    }

    @PostMapping("/{id}/messages")
    public Result sendMessage(@PathVariable Long id, @RequestBody ChatMessageRequest messageRequest, HttpServletRequest request) {
        UserAccount user = ensureSessionAccess(id, request);
        ensureOrderAccess(messageRequest.getOrderNo(), user);
        return Result.success(chatService.sendMessage(id, messageRequest));
    }

    @PostMapping(value = "/{id}/message-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@PathVariable Long id, @RequestBody ChatMessageRequest messageRequest, HttpServletRequest request) {
        UserAccount user = ensureSessionAccess(id, request);
        ensureOrderAccess(messageRequest.getOrderNo(), user);
        SseEmitter emitter = new SseEmitter(180_000L);
        CompletableFuture.runAsync(() -> {
            try {
                sendEvent(emitter, "progress", Map.of("stage", "RECEIVED", "message", "消息已发送，正在解析上下文"));
                sendEvent(emitter, "progress", Map.of("stage", "ANALYZING", "message", "正在识别意图、读取订单并检索知识库"));
                Map<String, Object> result = chatService.sendMessage(id, messageRequest);
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
    public Result listTraces(@PathVariable Long id, HttpServletRequest request) {
        ensureSessionAccess(id, request);
        return Result.success(chatService.listTraces(id));
    }

    @GetMapping(value = "/{id}/evidence-report", produces = "text/markdown; charset=UTF-8")
    public ResponseEntity<String> evidenceReport(@PathVariable Long id, HttpServletRequest request) {
        ensureSessionAccess(id, request);
        String markdown = chatService.buildEvidenceReport(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=session-" + id + "-evidence.md")
                .body(markdown);
    }

    private UserAccount ensureSessionAccess(Long id, HttpServletRequest request) {
        UserAccount user = requireRequestUser(request);
        if ("ADMIN".equals(user.getRole())) {
            return user;
        }
        Map<String, Object> detail = chatService.getDetail(id);
        Object owner = detail.get("userId");
        if (!(owner instanceof Number number) || number.longValue() != user.getId()) {
            throw new BusinessException("只能操作自己的会话");
        }
        return user;
    }

    private void ensureOrderAccess(String orderNo, UserAccount user) {
        if ("ADMIN".equals(user.getRole()) || orderNo == null || orderNo.isBlank()) {
            return;
        }
        DemoOrder order = orderService.getByOrderNo(orderNo);
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new BusinessException("只能咨询自己的订单");
        }
    }

    private UserAccount requireRequestUser(HttpServletRequest request) {
        return authService.requireUser(request.getHeader("Authorization"));
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(data));
    }
}
