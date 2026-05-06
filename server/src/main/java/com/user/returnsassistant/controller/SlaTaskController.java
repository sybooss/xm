package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.SlaTaskSearch;
import com.user.returnsassistant.service.SlaTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sla")
public class SlaTaskController {
    @Autowired
    private SlaTaskService slaTaskService;

    @GetMapping("/tasks")
    @OperatorAnno
    public Result tasks(SlaTaskSearch search) {
        return Result.success(slaTaskService.page(search));
    }
}
