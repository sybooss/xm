package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentRequest;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleRiskAssessmentService;
import com.user.returnsassistant.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AfterSaleRiskAssessmentController {
    @Autowired
    private AfterSaleRiskAssessmentService riskAssessmentService;
    @Autowired
    private AuthService authService;

    @PostMapping("/admin/after-sales/{id}/risk-assessment")
    @OperatorAnno
    public Result assess(@PathVariable Long id, @RequestBody(required = false) AfterSaleRiskAssessmentRequest assessmentRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request.getHeader("Authorization"));
        return Result.success(riskAssessmentService.assess(id, assessmentRequest, admin));
    }

    @GetMapping("/admin/after-sales/{id}/risk-assessment")
    @OperatorAnno
    public Result getByApplicationId(@PathVariable Long id, HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request.getHeader("Authorization"));
        return Result.success(riskAssessmentService.getByApplicationId(id, admin));
    }

    @GetMapping("/admin/risk-assessments")
    @OperatorAnno
    public Result page(AfterSaleRiskAssessmentSearch search) {
        return Result.success(riskAssessmentService.page(search));
    }
}
