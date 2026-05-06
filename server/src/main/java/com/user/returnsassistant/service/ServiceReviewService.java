package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.CustomerProfile;
import com.user.returnsassistant.pojo.ServiceReview;
import com.user.returnsassistant.pojo.ServiceReviewRequest;
import com.user.returnsassistant.pojo.UserAccount;

import java.util.List;

public interface ServiceReviewService {
    ServiceReview create(Long applicationId, ServiceReviewRequest request, UserAccount customer);

    ServiceReview getByApplicationId(Long applicationId, UserAccount customer);

    List<ServiceReview> listByUserId(Long userId);

    CustomerProfile customerProfile(Long userId);
}
