package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ServiceReview;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceReviewMapper {
    List<ServiceReview> listByUserId(@Param("userId") Long userId);

    ServiceReview getByApplicationId(@Param("applicationId") Long applicationId);

    void insert(ServiceReview review);

    long countByUserId(@Param("userId") Long userId);

    Double averageRatingByUserId(@Param("userId") Long userId);
}
