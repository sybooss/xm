package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.AfterSaleDiagnosisRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleDiagnosisService;
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
@RequestMapping
public class AfterSaleDiagnosisController {
    @Autowired
    private AfterSaleDiagnosisService diagnosisService;
    @Autowired
    private AuthService authService;

    @PostMapping("/after-sale-diagnoses")
    public Result diagnose(@RequestBody AfterSaleDiagnosisRequest diagnosisRequest, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(diagnosisService.diagnose(diagnosisRequest, user));
    }

    @GetMapping("/after-sale-diagnoses/{id}")
    public Result getById(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(diagnosisService.getById(id, user));
    }

    @GetMapping("/chat-sessions/{id}/after-sale-diagnoses/latest")
    public Result getLatestBySessionId(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(diagnosisService.getLatestBySessionId(id, user));
    }
}
