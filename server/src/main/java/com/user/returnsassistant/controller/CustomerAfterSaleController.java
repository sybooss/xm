package com.user.returnsassistant.controller;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.pojo.AfterSaleApplicationCreateRequest;
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
@RequestMapping("/customer/after-sales")
public class CustomerAfterSaleController {
    @Autowired
    private AfterSaleApplicationService afterSaleApplicationService;
    @Autowired
    private AuthService authService;

    @GetMapping
    public Result page(AfterSaleApplicationSearch search, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员请使用管理端售后接口");
        }
        search.setUserId(user.getId());
        return Result.success(afterSaleApplicationService.page(search));
    }

    @PostMapping
    public Result create(@RequestBody AfterSaleApplicationCreateRequest createRequest, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("管理员不能代替顾客从顾客端提交售后");
        }
        return Result.success(afterSaleApplicationService.create(createRequest, user));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        var application = afterSaleApplicationService.getById(id);
        if (!application.getUserId().equals(user.getId())) {
            throw new BusinessException("只能查看自己的售后申请");
        }
        return Result.success(application);
    }
}
