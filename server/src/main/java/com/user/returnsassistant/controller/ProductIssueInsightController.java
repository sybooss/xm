package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.ProductIssueInsightSearch;
import com.user.returnsassistant.pojo.ProductIssueRefreshRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.ProductIssueInsightService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/product-issue-insights")
public class ProductIssueInsightController {
    @Autowired
    private ProductIssueInsightService productIssueInsightService;
    @Autowired
    private AuthService authService;

    @GetMapping
    @OperatorAnno
    public Result page(ProductIssueInsightSearch search) {
        return Result.success(productIssueInsightService.page(search));
    }

    @GetMapping("/summary")
    @OperatorAnno
    public Result summary(@RequestParam(required = false) Integer days) {
        return Result.success(productIssueInsightService.summary(days));
    }

    @PostMapping("/refresh")
    @OperatorAnno
    public Result refresh(@RequestBody(required = false) ProductIssueRefreshRequest refreshRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request.getHeader("Authorization"));
        return Result.success(productIssueInsightService.refresh(refreshRequest, admin));
    }
}
