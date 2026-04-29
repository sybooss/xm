package com.user.returnsassistant.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatSessionSearch extends BaseSearch {
    private String status;
}
