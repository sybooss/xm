package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.ServiceReviewMapper;
import com.user.returnsassistant.mapper.ServiceTicketMapper;
import com.user.returnsassistant.mapper.UserAccountMapper;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.CustomerProfile;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import com.user.returnsassistant.pojo.ServiceReview;
import com.user.returnsassistant.pojo.ServiceReviewRequest;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.ServiceReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ServiceReviewServiceImpl implements ServiceReviewService {
    @Autowired
    private ServiceReviewMapper reviewMapper;
    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private ServiceTicketMapper ticketMapper;
    @Autowired
    private UserAccountMapper userMapper;

    @Transactional
    @Override
    public ServiceReview create(Long applicationId, ServiceReviewRequest request, UserAccount customer) {
        AfterSaleApplication application = requireApplication(applicationId);
        if (!Objects.equals(application.getUserId(), customer.getId())) {
            throw new BusinessException("只能评价自己的售后申请");
        }
        if (!"COMPLETED".equals(application.getStatus())) {
            throw new BusinessException("售后完成后才能评价");
        }
        if (reviewMapper.getByApplicationId(applicationId) != null) {
            throw new BusinessException("该售后申请已评价");
        }
        Integer rating = request == null ? null : request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评价分数必须在 1 到 5 之间");
        }
        ServiceReview review = new ServiceReview();
        review.setApplicationId(applicationId);
        review.setUserId(customer.getId());
        review.setRating(rating);
        review.setTags(cleanText(request.getTags(), 200));
        review.setComment(cleanText(request.getComment(), 500));
        reviewMapper.insert(review);
        writeLog(application, customer, "SUBMIT_REVIEW", "顾客提交服务评价：" + rating + " 分，" + nullToDash(review.getTags()));
        return reviewMapper.getByApplicationId(applicationId);
    }

    @Override
    public ServiceReview getByApplicationId(Long applicationId, UserAccount customer) {
        AfterSaleApplication application = requireApplication(applicationId);
        if (!Objects.equals(application.getUserId(), customer.getId())) {
            throw new BusinessException("只能查看自己的服务评价");
        }
        return reviewMapper.getByApplicationId(applicationId);
    }

    @Override
    public List<ServiceReview> listByUserId(Long userId) {
        return reviewMapper.listByUserId(userId);
    }

    @Override
    public CustomerProfile customerProfile(Long userId) {
        UserAccount customer = userMapper.getById(userId);
        if (customer == null || !"CUSTOMER".equals(customer.getRole())) {
            throw new BusinessException("客户不存在");
        }
        CustomerProfile profile = new CustomerProfile();
        profile.setCustomer(customer);

        OrderSearch orderSearch = new OrderSearch();
        orderSearch.setUserId(userId);
        orderSearch.setPage(1);
        orderSearch.setPageSize(5);
        List<DemoOrder> orders = orderMapper.page(orderSearch);
        profile.setRecentOrders(orders);
        profile.setOrderCount(orderMapper.count(orderSearch));
        profile.setTotalOrderAmount(defaultAmount(orderMapper.sumAmount(orderSearch)));

        AfterSaleApplicationSearch afterSaleSearch = new AfterSaleApplicationSearch();
        afterSaleSearch.setUserId(userId);
        afterSaleSearch.setPage(1);
        afterSaleSearch.setPageSize(8);
        List<AfterSaleApplication> afterSales = applicationMapper.page(afterSaleSearch);
        profile.setRecentAfterSales(afterSales);
        profile.setAfterSaleCount(applicationMapper.count(afterSaleSearch));
        profile.setActiveAfterSaleCount(applicationMapper.countActiveByUserId(userId));
        profile.setComplaintCount(applicationMapper.countByUserServiceType(userId, "COMPLAINT"));
        profile.setRecentAfterSaleCount(applicationMapper.countByUserSince(userId, 30));
        profile.setRepeatAfterSaleCount(Math.max(0, profile.getAfterSaleCount() - applicationMapper.countDistinctOrdersByUserId(userId)));
        profile.setComplaintRate(rate(profile.getComplaintCount(), profile.getAfterSaleCount()));

        List<ServiceTicket> tickets = ticketMapper.listByUserId(userId, 8);
        profile.setRecentTickets(tickets);
        profile.setTicketCount(ticketMapper.countByUserId(userId));

        List<ServiceReview> reviews = reviewMapper.listByUserId(userId);
        profile.setReviews(reviews);
        profile.setReviewCount(reviewMapper.countByUserId(userId));
        profile.setLowRatingCount(reviewMapper.countLowRatingByUserId(userId, 3));
        profile.setLowRatingReasons(lowRatingReasons(userId));
        profile.setAverageRating(reviewMapper.averageRatingByUserId(userId));
        profile.setRiskLevel(resolveRisk(profile));
        profile.setOperationSuggestion(buildOperationSuggestion(profile));
        return profile;
    }

    private AfterSaleApplication requireApplication(Long applicationId) {
        AfterSaleApplication application = applicationMapper.getById(applicationId);
        if (application == null) {
            throw new BusinessException("售后申请不存在");
        }
        return application;
    }

    private void writeLog(AfterSaleApplication application, UserAccount operator, String action, String remark) {
        AfterSaleProcessLog log = new AfterSaleProcessLog();
        log.setApplicationId(application.getId());
        log.setOperatorId(operator.getId());
        log.setOperatorName(operator.getDisplayName());
        log.setOperatorRole(operator.getRole());
        log.setAction(action);
        log.setFromStatus(application.getStatus());
        log.setToStatus(application.getStatus());
        log.setRemark(remark);
        processLogMapper.insert(log);
    }

    private String resolveRisk(CustomerProfile profile) {
        double avg = profile.getAverageRating() == null ? 5.0 : profile.getAverageRating();
        if (avg < 3.0 || profile.getAfterSaleCount() >= 4 || profile.getTicketCount() >= 3 || profile.getLowRatingCount() >= 2) {
            return "HIGH";
        }
        if (avg < 4.0 || profile.getAfterSaleCount() >= 2 || profile.getTicketCount() >= 1 || profile.getRecentAfterSaleCount() >= 2 || profile.getComplaintCount() >= 1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0 / denominator)) / 10.0;
    }

    private String lowRatingReasons(Long userId) {
        List<String> reasons = reviewMapper.lowRatingReasonsByUserId(userId, 3, 3).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
        return reasons.isEmpty() ? "暂无低分评价" : String.join("；", reasons);
    }

    private String buildOperationSuggestion(CustomerProfile profile) {
        if ("HIGH".equals(profile.getRiskLevel())) {
            return "建议安排资深客服跟进，优先解释处理结果、补齐证据链，并在完成后主动确认满意度。";
        }
        if ("MEDIUM".equals(profile.getRiskLevel())) {
            return "建议在回复中明确处理时限和下一步动作，减少重复咨询和重复投诉。";
        }
        return "按标准售后流程处理即可，持续记录凭证和评价反馈。";
    }

    private String cleanText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
