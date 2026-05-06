package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.ServiceTicketSearch;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AuthService;
import com.user.returnsassistant.service.AfterSaleApplicationService;
import com.user.returnsassistant.service.ChatService;
import com.user.returnsassistant.service.ServiceTicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ServiceTicketController {
    @Autowired
    private ServiceTicketService ticketService;
    @Autowired
    private AuthService authService;
    @Autowired
    private AfterSaleApplicationService afterSaleApplicationService;
    @Autowired
    private ChatService chatService;

    @GetMapping("/service-tickets")
    @OperatorAnno
    public Result page(ServiceTicketSearch search) {
        return Result.success(ticketService.page(search));
    }

    @PostMapping("/service-tickets")
    @OperatorAnno
    public Result save(@RequestBody ServiceTicket ticket) {
        return Result.success(ticketService.save(ticket));
    }

    @GetMapping("/service-tickets/{id}")
    @OperatorAnno
    public Result getById(@PathVariable Long id) {
        return Result.success(ticketService.getById(id));
    }

    @PutMapping("/service-tickets/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody ServiceTicket ticket, HttpServletRequest request) {
        UserAccount admin = authService.requireAdmin(request.getHeader("Authorization"));
        ticketService.update(id, ticket);
        ServiceTicket updated = ticketService.getById(id);
        afterSaleApplicationService.appendTicketProcessLog(id, "UPDATE_TICKET", "更新关联工单状态：" + updated.getStatus(), admin);
        return Result.success();
    }

    @DeleteMapping("/service-tickets/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }

    @GetMapping("/chat-sessions/{sessionId}/service-tickets")
    public Result listBySessionId(@PathVariable Long sessionId, HttpServletRequest request) {
        ensureSessionAccess(sessionId, request);
        return Result.success(ticketService.listBySessionId(sessionId));
    }

    @PostMapping("/chat-sessions/{sessionId}/service-tickets")
    @OperatorAnno
    public Result createBySession(@PathVariable Long sessionId, @RequestBody ServiceTicket ticket, HttpServletRequest request) {
        ensureSessionAccess(sessionId, request);
        ticket.setSessionId(sessionId);
        return Result.success(ticketService.save(ticket));
    }

    private void ensureSessionAccess(Long sessionId, HttpServletRequest request) {
        UserAccount user = authService.requireUser(request.getHeader("Authorization"));
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        Map<String, Object> detail = chatService.getDetail(sessionId);
        Object owner = detail.get("userId");
        if (!(owner instanceof Number number) || number.longValue() != user.getId()) {
            throw new BusinessException("只能操作自己的会话工单");
        }
    }
}
