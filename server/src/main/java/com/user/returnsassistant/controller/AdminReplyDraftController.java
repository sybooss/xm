package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.ReplyDraftActionRequest;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.ReplyDraftService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/after-sales/{applicationId}/reply-drafts")
public class AdminReplyDraftController {
    @Autowired
    private ReplyDraftService replyDraftService;
    @Autowired
    private AuthService authService;

    @GetMapping
    @OperatorAnno
    public Result list(@PathVariable Long applicationId) {
        return Result.success(replyDraftService.listByApplicationId(applicationId));
    }

    @PostMapping
    @OperatorAnno
    public Result generate(@PathVariable Long applicationId,
                           @RequestBody(required = false) ReplyDraftActionRequest actionRequest,
                           HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(replyDraftService.generate(applicationId, actionRequest, admin));
    }

    @PostMapping("/{draftId}/use")
    @OperatorAnno
    public Result use(@PathVariable Long applicationId,
                      @PathVariable Long draftId,
                      @RequestBody(required = false) ReplyDraftActionRequest actionRequest,
                      HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(replyDraftService.use(applicationId, draftId, actionRequest, admin));
    }

    @PostMapping("/{draftId}/discard")
    @OperatorAnno
    public Result discard(@PathVariable Long applicationId,
                          @PathVariable Long draftId,
                          @RequestBody(required = false) ReplyDraftActionRequest actionRequest,
                          HttpServletRequest request) {
        UserAccount admin = authService.requireUser(request.getHeader("Authorization"));
        return Result.success(replyDraftService.discard(applicationId, draftId, actionRequest, admin));
    }
}
