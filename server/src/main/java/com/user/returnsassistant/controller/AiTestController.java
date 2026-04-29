package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.AiTestRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai-tests")
public class AiTestController {
    @Autowired
    private AiService aiService;

    @PostMapping
    public Result test(@RequestBody AiTestRequest request) {
        return Result.success(aiService.test(request.getPrompt()));
    }
}
