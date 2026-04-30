package com.user.returnsassistant.controller;

import com.user.returnsassistant.anno.OperatorAnno;
import com.user.returnsassistant.pojo.Result;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.ServiceTicketSearch;
import com.user.returnsassistant.service.ServiceTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ServiceTicketController {
    @Autowired
    private ServiceTicketService ticketService;

    @GetMapping("/service-tickets")
    public Result page(ServiceTicketSearch search) {
        return Result.success(ticketService.page(search));
    }

    @PostMapping("/service-tickets")
    @OperatorAnno
    public Result save(@RequestBody ServiceTicket ticket) {
        return Result.success(ticketService.save(ticket));
    }

    @GetMapping("/service-tickets/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.success(ticketService.getById(id));
    }

    @PutMapping("/service-tickets/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody ServiceTicket ticket) {
        ticketService.update(id, ticket);
        return Result.success();
    }

    @DeleteMapping("/service-tickets/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }

    @GetMapping("/chat-sessions/{sessionId}/service-tickets")
    public Result listBySessionId(@PathVariable Long sessionId) {
        return Result.success(ticketService.listBySessionId(sessionId));
    }

    @PostMapping("/chat-sessions/{sessionId}/service-tickets")
    @OperatorAnno
    public Result createBySession(@PathVariable Long sessionId, @RequestBody ServiceTicket ticket) {
        ticket.setSessionId(sessionId);
        return Result.success(ticketService.save(ticket));
    }
}
