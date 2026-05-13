package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ManualReplyRequest {
    private String content;
    private Long useDraftId;
    private Boolean resolveTicket;
}
