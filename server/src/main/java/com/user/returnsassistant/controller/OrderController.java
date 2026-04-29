package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.AfterSaleRecordService;
import com.user.returnsassistant.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private AfterSaleRecordService afterSaleRecordService;

    @GetMapping
    public Result page(OrderSearch search) {
        return Result.success(orderService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody DemoOrder order) {
        orderService.save(order);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    @GetMapping("/no/{orderNo}")
    public Result getByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderService.getByOrderNo(orderNo));
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
    public Result listAfterSales(@PathVariable Long id) {
        return Result.success(afterSaleRecordService.listByOrderId(id));
    }
}
