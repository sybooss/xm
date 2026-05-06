package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.AfterSaleActionRequest;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleApplicationService;
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
@RequestMapping("/admin/after-sales")
public class AdminAfterSaleController {
    @Autowired
    private AfterSaleApplicationService afterSaleApplicationService;
    @Autowired
    private AuthService authService;

    @GetMapping
    @OperatorAnno
    public Result page(AfterSaleApplicationSearch search) {
        return Result.success(afterSaleApplicationService.page(search));
    }

    @GetMapping("/{id}")
    @OperatorAnno
    public Result getById(@PathVariable Long id) {
        return Result.success(afterSaleApplicationService.getById(id));
    }

    @PostMapping("/{id}/approve")
    @OperatorAnno
    public Result approve(@PathVariable Long id, @RequestBody AfterSaleActionRequest actionRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(afterSaleApplicationService.approve(id, actionRequest, admin));
    }

    @PostMapping("/{id}/reject")
    @OperatorAnno
    public Result reject(@PathVariable Long id, @RequestBody AfterSaleActionRequest actionRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(afterSaleApplicationService.reject(id, actionRequest, admin));
    }

    @PostMapping("/{id}/request-evidence")
    @OperatorAnno
    public Result requestEvidence(@PathVariable Long id, @RequestBody AfterSaleActionRequest actionRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(afterSaleApplicationService.requestEvidence(id, actionRequest, admin));
    }

    @PostMapping("/{id}/tickets")
    @OperatorAnno
    public Result createTicket(@PathVariable Long id, @RequestBody AfterSaleActionRequest actionRequest, HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(afterSaleApplicationService.createTicket(id, actionRequest, admin));
    }
}
