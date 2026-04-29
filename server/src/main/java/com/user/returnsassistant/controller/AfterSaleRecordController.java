package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.AfterSaleRecordSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.AfterSaleRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/after-sale-records")
public class AfterSaleRecordController {
    @Autowired
    private AfterSaleRecordService recordService;

    @GetMapping
    public Result page(AfterSaleRecordSearch search) {
        return Result.success(recordService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody AfterSaleRecord record) {
        recordService.save(record);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(recordService.getById(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody AfterSaleRecord record) {
        recordService.update(id, record);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        recordService.delete(id);
        return Result.success();
    }
}
