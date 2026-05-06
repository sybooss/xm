package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ServiceReview;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ServiceReviewMapper {
    List<ServiceReview> listByUserId(@Param("userId") Long userId);

    ServiceReview getByApplicationId(@Param("applicationId") Long applicationId);

    void insert(ServiceReview review);

    long countByUserId(@Param("userId") Long userId);

    long countLowRatingByUserId(@Param("userId") Long userId, @Param("maxRating") Integer maxRating);

    List<String> lowRatingReasonsByUserId(@Param("userId") Long userId, @Param("maxRating") Integer maxRating, @Param("limit") Integer limit);

    Double averageRatingByUserId(@Param("userId") Long userId);
}
