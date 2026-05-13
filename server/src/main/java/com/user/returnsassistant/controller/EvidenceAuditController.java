package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.EvidenceAuditRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.EvidenceAuditService;
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
public class EvidenceAuditController {
    @Autowired
    private EvidenceAuditService evidenceAuditService;
    @Autowired
    private AuthService authService;

    @PostMapping("/after-sale-evidences/{id}/audits")
    public Result audit(@PathVariable Long id, @RequestBody(required = false) EvidenceAuditRequest auditRequest, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(evidenceAuditService.audit(id, auditRequest, user));
    }

    @GetMapping("/after-sale-evidences/{id}/audits")
    public Result listByEvidenceId(@PathVariable Long id, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(evidenceAuditService.listByEvidenceId(id, user));
    }

    @GetMapping("/admin/after-sales/{id}/evidence-audits")
    @OperatorAnno
    public Result listByApplicationId(@PathVariable Long id, HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request.getHeader("Authorization"));
        return Result.success(evidenceAuditService.listByApplicationId(id, admin));
    }
}
