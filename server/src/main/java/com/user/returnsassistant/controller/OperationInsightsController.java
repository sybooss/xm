package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.OperationInsightsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationInsightsController {
    @Autowired
    private OperationInsightsService operationInsightsService;

    @GetMapping("/operation-insights")
    @OperatorAnno
    public Result insights() {
        return Result.success(operationInsightsService.getInsights());
    }
}
