package com.user.returnsassistant.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocSearch extends BaseSearch {
    private Long categoryId;
    private String docType;
    private String intentCode;
    private String status;
}
