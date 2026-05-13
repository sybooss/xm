package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ProductIssueRefreshResult {
    private Integer refreshedCount;
    private Integer highCount;
    private Integer mediumCount;
    private ProductIssueAlert topAlert;
}
