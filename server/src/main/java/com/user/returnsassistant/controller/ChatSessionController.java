package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.ChatMessageRequest;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.ChatSessionSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}/process-traces")
    public Result listTraces(@PathVariable Long id) {
        return Result.success(chatService.listTraces(id));
    }
}
