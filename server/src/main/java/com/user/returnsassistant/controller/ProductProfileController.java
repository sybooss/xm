package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.ProductProfile;
import com.user.returnsassistant.pojo.ProductProfileSearch;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.ProductProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-profiles")
public class ProductProfileController {
    @Autowired
    private ProductProfileService profileService;

    @GetMapping
    public Result page(ProductProfileSearch search) {
        return Result.success(profileService.page(search));
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody ProductProfile profile) {
        profileService.save(profile);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(profileService.getById(id));
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody ProductProfile profile) {
        profileService.update(id, profile);
        return Result.success();
    }
}
