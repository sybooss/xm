package com.user.returnsassistant.controller;

import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.EvidenceFileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/customer/evidence-files")
public class EvidenceUploadController {
    @Autowired
    private EvidenceFileService evidenceFileService;
    @Autowired
    private AuthService authService;

    @PostMapping
    public Result upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        UserAccount customer = authService.requireCustomer(request.getHeader("Authorization"));
        return Result.success(evidenceFileService.upload(file, customer));
    }
}
