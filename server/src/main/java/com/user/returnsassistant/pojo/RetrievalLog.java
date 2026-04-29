package com.user.returnsassistant.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RetrievalLog {
    private Long id;
    private Long sessionId;
    private Long messageId;
    private String queryText;
    private Long docId;
    private Integer rankNo;
    private BigDecimal score;
    private String hitReason;
    private String docTitleSnapshot;
    private String docContentSnapshot;
    private LocalDateTime createdAt;
}
