package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.ProductInsightRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.OrderService;
import com.user.returnsassistant.service.ProductInsightService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductInsightController {
    @Autowired
    private ProductInsightService productInsightService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private AuthService authService;

    @GetMapping("/orders/{id}/product-insight")
    public Result getByOrderId(@PathVariable Long id,
                               @RequestParam(required = false) String issueText,
                               @RequestParam(required = false) Boolean useAi,
                               HttpServletRequest request) {
        DemoOrder order = orderService.getById(id);
        ensureOrderAccess(order, request);
        return Result.success(productInsightService.buildByOrderId(id, issueText, useAi));
    }

    @GetMapping("/orders/no/{orderNo}/product-insight")
    public Result getByOrderNo(@PathVariable String orderNo,
                               @RequestParam(required = false) String issueText,
                               @RequestParam(required = false) Boolean useAi,
                               HttpServletRequest request) {
        DemoOrder order = orderService.getByOrderNo(orderNo);
        ensureOrderAccess(order, request);
        return Result.success(productInsightService.buildByOrderNo(orderNo, issueText, useAi));
    }

    @PostMapping("/product-insights")
    public Result createInsight(@RequestBody ProductInsightRequest requestBody, HttpServletRequest request) {
        DemoOrder order = requestBody.getOrderId() != null
                ? orderService.getById(requestBody.getOrderId())
                : orderService.getByOrderNo(requestBody.getOrderNo());
        ensureOrderAccess(order, request);
        return Result.success(productInsightService.build(order, requestBody.getIssueText(), null, requestBody.getUseAi()));
    }

    private void ensureOrderAccess(DemoOrder order, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        authService.ensureSelfOrAdmin(user, order.getUserId(), "只能查看自己的订单产品洞察");
    }
}
