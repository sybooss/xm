package com.user.returnsassistant.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AfterSaleProcessLog {
    private Long id;
    private Long applicationId;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime createdAt;
}
