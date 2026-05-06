package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class AfterSaleEvidenceRequest {
    private String evidenceType;
    private String fileUrl;
    private String content;
}
