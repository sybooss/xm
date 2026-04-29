package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.KnowledgeDocSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.KnowledgeDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge-docs")
public class KnowledgeDocController {
    @Autowired
    private KnowledgeDocService docService;

    @GetMapping
    public Result page(KnowledgeDocSearch search) {
        return Result.success(docService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody KnowledgeDoc doc) {
        docService.save(doc);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(docService.getById(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody KnowledgeDoc doc) {
        docService.update(id, doc);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        docService.delete(id);
        return Result.success();
    }

    @GetMapping("/search")
    public Result search(String query, String intentCode, Integer limit) {
        return Result.success(docService.search(query, intentCode, limit));
    }
}
