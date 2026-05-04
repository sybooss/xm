package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleRecordService;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private AfterSaleRecordService afterSaleRecordService;
    @Autowired
    private AuthService authService;

    @GetMapping
    public Result page(OrderSearch search, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        if (!"ADMIN".equals(user.getRole())) {
            search.setUserId(user.getId());
        }
        return Result.success(orderService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody DemoOrder order) {
        orderService.save(order);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id, HttpServletRequest request) {
        DemoOrder order = orderService.getById(id);
        ensureOrderOwner(order, authService.requireUser(request.getHeader("Authorization")));
        return Result.success(order);
    }

    @GetMapping("/no/{orderNo}")
    public Result getByOrderNo(@PathVariable String orderNo, HttpServletRequest request) {
        DemoOrder order = orderService.getByOrderNo(orderNo);
        ensureOrderOwner(order, authService.requireUser(request.getHeader("Authorization")));
        return Result.success(order);
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody DemoOrder order) {
        orderService.update(id, order);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        orderService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/after-sale-records")
    public Result listAfterSales(@PathVariable Long id, HttpServletRequest request) {
        DemoOrder order = orderService.getById(id);
        ensureOrderOwner(order, authService.requireUser(request.getHeader("Authorization")));
        return Result.success(afterSaleRecordService.listByOrderId(id));
    }

    @PostMapping("/{id}/after-sale-records")
    public Result applyAfterSale(@PathVariable Long id, @RequestBody AfterSaleRecord record, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        DemoOrder order = orderService.getById(id);
        if (!"ADMIN".equals(user.getRole()) && !Objects.equals(order.getUserId(), user.getId())) {
            throw new BusinessException("只能为自己的订单申请售后");
        }
        if (afterSaleRecordService.listByOrderId(id).stream().anyMatch(this::isActiveAfterSale)) {
            throw new BusinessException("该订单已有进行中的售后申请，请勿重复提交");
        }
        record.setOrderId(id);
        if (record.getServiceType() == null || record.getServiceType().isBlank()) {
            record.setServiceType("RETURN");
        }
        record.setStatus("APPLIED");
        afterSaleRecordService.save(record);
        return Result.success(record);
    }

    private boolean isActiveAfterSale(AfterSaleRecord record) {
        if (record == null || record.getStatus() == null) {
            return true;
        }
        return !"REJECTED".equals(record.getStatus()) && !"FINISHED".equals(record.getStatus());
    }

    private void ensureOrderOwner(DemoOrder order, UserAccount user) {
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        if (!Objects.equals(order.getUserId(), user.getId())) {
            throw new BusinessException("只能查看自己的订单");
        }
    }
}
