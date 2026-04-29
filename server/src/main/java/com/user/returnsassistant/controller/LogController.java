package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {
    @Autowired
    private LogService logService;

    @GetMapping("/ai-call-logs")
    public Result aiLogs(Integer page, Integer pageSize, String status) {
        return Result.success(logService.pageAiLogs(page, pageSize, status));
    }

    @GetMapping("/retrieval-logs")
    public Result retrievalLogs(Integer page, Integer pageSize, String keyword) {
        return Result.success(logService.pageRetrievalLogs(page, pageSize, keyword));
    }
}
