package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.KnowledgeCategory;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.KnowledgeCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge-categories")
public class KnowledgeCategoryController {
    @Autowired
    private KnowledgeCategoryService categoryService;

    @GetMapping
    public Result list(Integer enabled) {
        return Result.success(categoryService.list(enabled));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody KnowledgeCategory category) {
        categoryService.save(category);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody KnowledgeCategory category) {
        categoryService.update(id, category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
