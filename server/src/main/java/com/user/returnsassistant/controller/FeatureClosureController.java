package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.FeatureClosureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureClosureController {
    @Autowired
    private FeatureClosureService featureClosureService;

    @GetMapping("/feature-closures")
    @OperatorAnno
    public Result dashboard() {
        return Result.success(featureClosureService.getDashboard());
    }
}
