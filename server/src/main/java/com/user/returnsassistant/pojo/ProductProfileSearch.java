package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ProductProfileSearch extends BaseSearch {
    private String category;
    private Integer enabled;
}
