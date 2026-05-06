package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.ServiceReviewRequest;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.ServiceReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceReviewController {
    @Autowired
    private ServiceReviewService reviewService;
    @Autowired
    private AuthService authService;

    @PostMapping("/customer/after-sales/{applicationId}/reviews")
    public Result create(@PathVariable Long applicationId,
                         @RequestBody ServiceReviewRequest reviewRequest,
                         HttpServletRequest request) {
        UserAccount customer = authService.requireCustomer(request.getHeader("Authorization"));
        return Result.success(reviewService.create(applicationId, reviewRequest, customer));
    }

    @GetMapping("/customer/after-sales/{applicationId}/reviews")
    public Result getByApplicationId(@PathVariable Long applicationId, HttpServletRequest request) {
        UserAccount customer = authService.requireCustomer(request.getHeader("Authorization"));
        return Result.success(reviewService.getByApplicationId(applicationId, customer));
    }

    @GetMapping("/admin/customers/{userId}/profile")
    @OperatorAnno
    public Result customerProfile(@PathVariable Long userId) {
        return Result.success(reviewService.customerProfile(userId));
    }

    @GetMapping("/admin/customers/{userId}/reviews")
    @OperatorAnno
    public Result customerReviews(@PathVariable Long userId) {
        return Result.success(reviewService.listByUserId(userId));
    }
}
