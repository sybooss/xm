package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.ChatMessage;
import com.user.returnsassistant.pojo.ManualReplyRequest;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.UserAccount;

import java.util.List;

public interface ManualReplyService {
    List<ChatMessage> listConversation(Long ticketId);

    ServiceTicket takeOver(Long ticketId, UserAccount admin);

    ChatMessage sendManualReply(Long ticketId, ManualReplyRequest request, UserAccount admin);
}
