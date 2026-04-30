package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.ChatSessionMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.ServiceTicketMapper;
import com.user.returnsassistant.pojo.*;
import com.user.returnsassistant.service.ServiceTicketService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceTicketServiceImpl implements ServiceTicketService {
    @Autowired
    private ServiceTicketMapper ticketMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private DemoOrderMapper orderMapper;

    @Override
    public PageResult<ServiceTicket> page(ServiceTicketSearch search) {
        return new PageResult<>(ticketMapper.count(search), ticketMapper.page(search));
    }

    @Override
    public ServiceTicket getById(Long id) {
        ServiceTicket ticket = ticketMapper.getById(id);
        if (ticket == null) {
            throw new BusinessException("工单不存在");
        }
        return ticket;
    }

    @Override
    public List<ServiceTicket> listBySessionId(Long sessionId) {
        return ticketMapper.listBySessionId(sessionId);
    }

    @Transactional
    @Override
    public ServiceTicket save(ServiceTicket ticket) {
        if (ticket.getSessionId() == null) {
            throw new BusinessException("会话不能为空");
        }
        ChatSession session = sessionMapper.getById(ticket.getSessionId());
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (ticket.getOrderNo() != null && !ticket.getOrderNo().isBlank()) {
            DemoOrder order = orderMapper.getByOrderNo(ticket.getOrderNo());
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            ticket.setOrderId(order.getId());
        } else if (ticket.getOrderId() == null) {
            ticket.setOrderId(session.getOrderId());
        }
        ticket.setUserId(ticket.getUserId() == null ? session.getUserId() : ticket.getUserId());
        prepareDefault(ticket);
        ticketMapper.insert(ticket);
        return getById(ticket.getId());
    }

    @Override
    public void update(Long id, ServiceTicket ticket) {
        ServiceTicket old = getById(id);
        ticket.setId(id);
        if (ticket.getPriority() == null) {
            ticket.setPriority(old.getPriority());
        }
        if (ticket.getStatus() == null) {
            ticket.setStatus(old.getStatus());
        }
        if (ticket.getCustomerIssue() == null) {
            ticket.setCustomerIssue(old.getCustomerIssue());
        }
        if (ticket.getAiSummary() == null) {
            ticket.setAiSummary(old.getAiSummary());
        }
        if (ticket.getSuggestedAction() == null) {
            ticket.setSuggestedAction(old.getSuggestedAction());
        }
        if (ticket.getAssignedTo() == null) {
            ticket.setAssignedTo(old.getAssignedTo());
        }
        ticketMapper.update(ticket);
    }

    @Override
    public void delete(Long id) {
        ticketMapper.delete(id);
    }

    @Transactional
    @Override
    public TicketResult createFromSession(ChatSession session, DemoOrder order, Long messageId, String intentCode,
                                          String customerIssue, String aiSummary, String suggestedAction, boolean forceNew) {
        if (session == null || session.getId() == null) {
            throw new BusinessException("会话不存在");
        }
        if (!forceNew) {
            ServiceTicket open = ticketMapper.getOpenBySessionId(session.getId());
            if (open != null) {
                return new TicketResult(false, open, "该会话已有待处理人工工单");
            }
        }

        ServiceTicket ticket = new ServiceTicket();
        ticket.setSessionId(session.getId());
        ticket.setSessionNo(session.getSessionNo());
        ticket.setMessageId(messageId);
        ticket.setOrderId(order == null ? session.getOrderId() : order.getId());
        ticket.setOrderNo(order == null ? session.getOrderNo() : order.getOrderNo());
        ticket.setUserId(session.getUserId());
        ticket.setIntentCode(intentCode);
        ticket.setCustomerIssue(customerIssue);
        ticket.setAiSummary(aiSummary);
        ticket.setSuggestedAction(suggestedAction);
        ticket.setPriority(resolvePriority(intentCode, customerIssue, order));
        prepareDefault(ticket);
        ticketMapper.insert(ticket);
        return new TicketResult(true, getById(ticket.getId()), "已创建人工客服工单");
    }

    private void prepareDefault(ServiceTicket ticket) {
        if (ticket.getTicketNo() == null || ticket.getTicketNo().isBlank()) {
            ticket.setTicketNo(NoUtils.ticketNo());
        }
        if (ticket.getPriority() == null || ticket.getPriority().isBlank()) {
            ticket.setPriority("NORMAL");
        }
        if (ticket.getStatus() == null || ticket.getStatus().isBlank()) {
            ticket.setStatus("PENDING");
        }
        if (ticket.getCustomerIssue() == null || ticket.getCustomerIssue().isBlank()) {
            ticket.setCustomerIssue("用户请求人工客服处理");
        }
    }

    private String resolvePriority(String intentCode, String customerIssue, DemoOrder order) {
        String text = customerIssue == null ? "" : customerIssue;
        if (containsAny(text, "丢", "损坏", "报警", "严重")) {
            return "URGENT";
        }
        if ("COMPLAINT_TRANSFER".equals(intentCode) || containsAny(text, "投诉", "人工", "不处理", "介入")) {
            return "HIGH";
        }
        if (order != null && "ABNORMAL".equals(order.getLogisticsStatus())) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
