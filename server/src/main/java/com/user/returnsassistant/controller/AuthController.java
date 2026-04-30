package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.LoginRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public Result me(HttpServletRequest request) {
        return Result.success(authService.me(request.getHeader("Authorization")));
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return Result.success();
    }
}
