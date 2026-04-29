package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class BaseSearch {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String keyword;

    public int getOffset() {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return (currentPage - 1) * size;
    }

    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
