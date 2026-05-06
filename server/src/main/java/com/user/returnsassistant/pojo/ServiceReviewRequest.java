package com.user.returnsassistant.pojo;

import lombok.Data;

@Data
public class ServiceReviewRequest {
    private Integer rating;
    private String tags;
    private String comment;
}
