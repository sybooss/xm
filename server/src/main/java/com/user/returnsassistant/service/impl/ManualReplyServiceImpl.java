package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.ChatMessageMapper;
import com.user.returnsassistant.mapper.ReplyDraftMapper;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.ChatMessage;
import com.user.returnsassistant.pojo.ManualReplyRequest;
import com.user.returnsassistant.pojo.ReplyDraft;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleApplicationService;
import com.user.returnsassistant.service.ChatImageRiskService;
import com.user.returnsassistant.service.ManualReplyService;
import com.user.returnsassistant.service.ServiceTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ManualReplyServiceImpl implements ManualReplyService {
    private static final Set<String> CLOSED_TICKET_STATUSES = Set.of("RESOLVED", "CLOSED");

    @Autowired
    private ServiceTicketService ticketService;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private ReplyDraftMapper replyDraftMapper;
    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleApplicationService afterSaleApplicationService;
    @Autowired
    private ChatImageRiskService chatImageRiskService;

    @Override
    public List<ChatMessage> listConversation(Long ticketId) {
        ServiceTicket ticket = requireReplyableTicket(ticketId);
        List<ChatMessage> messages = messageMapper.listBySessionId(ticket.getSessionId());
        chatImageRiskService.attachRisks(ticket.getSessionId(), messages);
        return messages;
    }

    @Transactional
    @Override
    public ServiceTicket takeOver(Long ticketId, UserAccount admin) {
        ServiceTicket ticket = requireReplyableTicket(ticketId);
        if (CLOSED_TICKET_STATUSES.contains(ticket.getStatus())) {
            throw new BusinessException("工单已结束，不能接管");
        }
        ServiceTicket update = new ServiceTicket();
        update.setStatus("PROCESSING");
        update.setAssignedTo(displayName(admin));
        ticketService.update(ticketId, update);
        afterSaleApplicationService.appendTicketProcessLog(ticketId, "MANUAL_TAKEOVER",
                displayName(admin) + " 接管人工客服工单", admin);
        return ticketService.getById(ticketId);
    }

    @Transactional
    @Override
    public ChatMessage sendManualReply(Long ticketId, ManualReplyRequest request, UserAccount admin) {
        ServiceTicket ticket = requireReplyableTicket(ticketId);
        if (CLOSED_TICKET_STATUSES.contains(ticket.getStatus())) {
            throw new BusinessException("工单已结束，不能发送人工回复");
        }
        String content = cleanContent(request == null ? null : request.getContent());

        ReplyDraft draft = null;
        if (request != null && request.getUseDraftId() != null) {
            draft = requireUsableDraft(ticket, request.getUseDraftId());
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(ticket.getSessionId());
        message.setRole("ASSISTANT");
        message.setContent(content);
        message.setMessageType("TEXT");
        message.setSeqNo(messageMapper.maxSeqNo(ticket.getSessionId()) + 1);
        message.setIntentCode(ticket.getIntentCode());
        message.setSourceType("MANUAL");
        messageMapper.insert(message);

        ServiceTicket update = new ServiceTicket();
        update.setStatus(Boolean.TRUE.equals(request == null ? null : request.getResolveTicket()) ? "RESOLVED" : "PROCESSING");
        update.setAssignedTo(displayName(admin));
        ticketService.update(ticketId, update);

        if (draft != null) {
            replyDraftMapper.updateStatus(draft.getId(), "USED",
                    "人工客服发送回复时采纳草稿：" + preview(content), true);
            afterSaleApplicationService.appendTicketProcessLog(ticketId, "USE_REPLY_DRAFT",
                    "人工回复采用 AI 草稿，摘要：" + preview(content), admin);
        }
        afterSaleApplicationService.appendTicketProcessLog(ticketId, "MANUAL_REPLY_SENT",
                "人工客服回复：" + preview(content), admin);
        return messageMapper.listBySessionId(ticket.getSessionId()).stream()
                .filter(item -> Objects.equals(item.getId(), message.getId()))
                .findFirst()
                .orElse(message);
    }

    private ServiceTicket requireReplyableTicket(Long ticketId) {
        ServiceTicket ticket = ticketService.getById(ticketId);
        if (ticket.getSessionId() == null) {
            throw new BusinessException("工单未关联会话，不能发送人工回复");
        }
        return ticket;
    }

    private ReplyDraft requireUsableDraft(ServiceTicket ticket, Long draftId) {
        ReplyDraft draft = replyDraftMapper.getById(draftId);
        if (draft == null) {
            throw new BusinessException("回复草稿不存在");
        }
        AfterSaleApplication application = applicationMapper.getByTicketId(ticket.getId());
        if (application == null) {
            if (!Objects.equals(ticket.getId(), draft.getTicketId())) {
                throw new BusinessException("回复草稿不属于当前工单");
            }
        } else if (!Objects.equals(application.getId(), draft.getApplicationId())) {
            throw new BusinessException("回复草稿不属于当前工单关联的售后申请");
        }
        if (draft.getTicketId() != null && !Objects.equals(ticket.getId(), draft.getTicketId())) {
            throw new BusinessException("回复草稿不属于当前工单");
        }
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new BusinessException("回复草稿已经处理，不能重复采纳");
        }
        return draft;
    }

    private String cleanContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("人工回复内容不能为空");
        }
        String value = content.trim();
        if (value.length() > 2000) {
            throw new BusinessException("人工回复内容不能超过 2000 字");
        }
        return value;
    }

    private String displayName(UserAccount admin) {
        if (admin == null) {
            return "管理员";
        }
        if (admin.getDisplayName() != null && !admin.getDisplayName().isBlank()) {
            return admin.getDisplayName();
        }
        return admin.getUsername() == null || admin.getUsername().isBlank() ? "管理员" : admin.getUsername();
    }

    private String preview(String content) {
        String value = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }
}
