package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.ReplyDraft;
import com.user.returnsassistant.pojo.ReplyDraftActionRequest;
import com.user.returnsassistant.pojo.UserAccount;

import java.util.List;

public interface ReplyDraftService {
    List<ReplyDraft> listByApplicationId(Long applicationId);

    ReplyDraft generate(Long applicationId, ReplyDraftActionRequest request, UserAccount admin);

    ReplyDraft use(Long applicationId, Long draftId, ReplyDraftActionRequest request, UserAccount admin);

    ReplyDraft discard(Long applicationId, Long draftId, ReplyDraftActionRequest request, UserAccount admin);
}
