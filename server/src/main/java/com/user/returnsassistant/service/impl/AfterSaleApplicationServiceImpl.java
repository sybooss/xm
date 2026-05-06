package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.pojo.AfterSaleActionRequest;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleApplicationCreateRequest;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleApplicationService;
import com.user.returnsassistant.utils.NoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AfterSaleApplicationServiceImpl implements AfterSaleApplicationService {
    private static final Set<String> SERVICE_TYPES = Set.of("RETURN", "EXCHANGE", "REFUND", "COMPLAINT");
    private static final Set<String> REVIEWABLE_STATUSES = Set.of("SUBMITTED", "UNDER_REVIEW");
    private static final Set<String> TERMINAL_STATUSES = Set.of("REJECTED", "COMPLETED", "CANCELLED");

    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;
    @Autowired
    private DemoOrderMapper orderMapper;

    @Override
    public PageResult<AfterSaleApplication> page(AfterSaleApplicationSearch search) {
        return new PageResult<>(applicationMapper.count(search), applicationMapper.page(search));
    }

    @Override
    public AfterSaleApplication getById(Long id) {
        AfterSaleApplication application = applicationMapper.getById(id);
        if (application == null) {
            throw new BusinessException("售后申请不存在");
        }
        application.setProcessLogs(processLogMapper.listByApplicationId(id));
        return application;
    }

    @Transactional
    @Override
    public AfterSaleApplication create(AfterSaleApplicationCreateRequest request, UserAccount customer) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException("请选择需要申请售后的订单");
        }
        DemoOrder order = orderMapper.getById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"ADMIN".equals(customer.getRole()) && !Objects.equals(order.getUserId(), customer.getId())) {
            throw new BusinessException("只能为自己的订单申请售后");
        }
        if (!"PAID".equals(order.getPayStatus())) {
            throw new BusinessException("未支付订单不能申请售后");
        }
        if ("PENDING_PAY".equals(order.getOrderStatus()) || "CLOSED".equals(order.getOrderStatus())) {
            throw new BusinessException("当前订单状态不能申请售后");
        }
        if (applicationMapper.countActiveByOrderId(order.getId()) > 0) {
            throw new BusinessException("该订单已有进行中的售后申请");
        }

        String serviceType = normalizeServiceType(request.getServiceType());
        BigDecimal refundAmount = normalizeAmount(request.getRefundAmount());
        if (refundAmount != null && order.getOrderAmount() != null && refundAmount.compareTo(order.getOrderAmount()) > 0) {
            throw new BusinessException("申请退款金额不能超过订单实付金额");
        }

        AfterSaleApplication application = new AfterSaleApplication();
        application.setApplicationNo(NoUtils.afterSaleNo());
        application.setOrderId(order.getId());
        application.setUserId(order.getUserId());
        application.setServiceType(serviceType);
        application.setReasonCode(hasText(request.getReasonCode()) ? request.getReasonCode().trim() : "OTHER");
        application.setReasonText(cleanRequired(request.getReasonText(), "请填写售后原因说明"));
        application.setStatus("SUBMITTED");
        application.setRefundAmount(refundAmount);
        application.setApprovedAmount(null);
        application.setPriority(resolvePriority(serviceType, refundAmount, order.getOrderAmount()));
        application.setSlaDeadline(LocalDateTime.now().plusHours("COMPLAINT".equals(serviceType) ? 12 : 48));
        application.setRiskLevel(resolveRiskLevel(serviceType, refundAmount, order.getOrderAmount()));
        applicationMapper.insert(application);
        writeLog(application.getId(), customer, "SUBMIT", null, "SUBMITTED", "顾客提交售后申请：" + application.getReasonText());
        orderMapper.updateAfterSaleStatus(order.getId(), toOrderAfterSaleStatus(serviceType, "SUBMITTED"));
        return getById(application.getId());
    }

    @Transactional
    @Override
    public AfterSaleApplication approve(Long id, AfterSaleActionRequest request, UserAccount admin) {
        AfterSaleApplication application = getById(id);
        ensureReviewable(application);

        BigDecimal approvedAmount = request == null ? null : normalizeAmount(request.getApprovedAmount());
        if (approvedAmount == null) {
            approvedAmount = application.getRefundAmount();
        }
        if (approvedAmount != null && application.getRefundAmount() != null && approvedAmount.compareTo(application.getRefundAmount()) > 0) {
            throw new BusinessException("审核通过金额不能超过申请金额");
        }

        String toStatus = nextApprovedStatus(application.getServiceType());
        AfterSaleApplication update = new AfterSaleApplication();
        update.setId(id);
        update.setStatus(toStatus);
        update.setApprovedAmount(approvedAmount);
        update.setAssignedTo(request == null ? null : request.getAssignedTo());
        applicationMapper.updateDecision(update);
        writeLog(id, admin, "APPROVE", application.getStatus(), toStatus, cleanRemark(request, "管理员审核通过售后申请"));
        orderMapper.updateAfterSaleStatus(application.getOrderId(), toOrderAfterSaleStatus(application.getServiceType(), toStatus));
        return getById(id);
    }

    @Transactional
    @Override
    public AfterSaleApplication reject(Long id, AfterSaleActionRequest request, UserAccount admin) {
        AfterSaleApplication application = getById(id);
        ensureReviewable(application);
        String remark = cleanRequired(request == null ? null : request.getRemark(), "驳回售后申请必须填写原因");

        AfterSaleApplication update = new AfterSaleApplication();
        update.setId(id);
        update.setStatus("REJECTED");
        update.setApprovedAmount(application.getApprovedAmount());
        update.setAssignedTo(application.getAssignedTo());
        update.setClosedAt(LocalDateTime.now());
        applicationMapper.updateDecision(update);
        writeLog(id, admin, "REJECT", application.getStatus(), "REJECTED", remark);
        orderMapper.updateAfterSaleStatus(application.getOrderId(), "REJECTED");
        return getById(id);
    }

    private void ensureReviewable(AfterSaleApplication application) {
        if (TERMINAL_STATUSES.contains(application.getStatus()) || !REVIEWABLE_STATUSES.contains(application.getStatus())) {
            throw new BusinessException("当前状态不能执行审核动作");
        }
    }

    private void writeLog(Long applicationId, UserAccount operator, String action, String fromStatus, String toStatus, String remark) {
        AfterSaleProcessLog log = new AfterSaleProcessLog();
        log.setApplicationId(applicationId);
        log.setOperatorId(operator.getId());
        log.setOperatorName(operator.getDisplayName());
        log.setOperatorRole(operator.getRole());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        processLogMapper.insert(log);
    }

    private String normalizeServiceType(String serviceType) {
        String value = hasText(serviceType) ? serviceType.trim().toUpperCase() : "RETURN";
        if (!SERVICE_TYPES.contains(value)) {
            throw new BusinessException("售后类型不正确");
        }
        return value;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("金额不能小于 0");
        }
        return amount;
    }

    private String cleanRequired(String value, String message) {
        if (!hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String cleanRemark(AfterSaleActionRequest request, String fallback) {
        if (request != null && hasText(request.getRemark())) {
            return request.getRemark().trim();
        }
        return fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolvePriority(String serviceType, BigDecimal refundAmount, BigDecimal orderAmount) {
        if ("COMPLAINT".equals(serviceType)) {
            return "URGENT";
        }
        BigDecimal compareAmount = refundAmount == null ? orderAmount : refundAmount;
        if (compareAmount != null && compareAmount.compareTo(new BigDecimal("300.00")) >= 0) {
            return "HIGH";
        }
        return "NORMAL";
    }

    private String resolveRiskLevel(String serviceType, BigDecimal refundAmount, BigDecimal orderAmount) {
        if ("COMPLAINT".equals(serviceType)) {
            return "HIGH";
        }
        BigDecimal compareAmount = refundAmount == null ? orderAmount : refundAmount;
        if (compareAmount != null && compareAmount.compareTo(new BigDecimal("300.00")) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String nextApprovedStatus(String serviceType) {
        return switch (serviceType) {
            case "REFUND" -> "REFUNDING";
            case "EXCHANGE" -> "EXCHANGING";
            case "COMPLAINT" -> "APPROVED";
            default -> "WAIT_BUYER_SEND";
        };
    }

    private String toOrderAfterSaleStatus(String serviceType, String status) {
        if ("REJECTED".equals(status)) {
            return "REJECTED";
        }
        if ("REFUNDING".equals(status)) {
            return "REFUNDING";
        }
        if ("EXCHANGE".equals(serviceType) || "EXCHANGING".equals(status)) {
            return "EXCHANGE_APPLYING";
        }
        if ("COMPLAINT".equals(serviceType)) {
            return "RETURN_APPLYING";
        }
        return "RETURN_APPLYING";
    }
}
