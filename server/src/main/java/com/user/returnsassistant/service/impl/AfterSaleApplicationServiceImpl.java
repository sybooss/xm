package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleApplicationMapper;
import com.user.returnsassistant.mapper.AfterSaleEvidenceMapper;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.ChatSessionMapper;
import com.user.returnsassistant.mapper.DemoOrderMapper;
import com.user.returnsassistant.mapper.ServiceTicketMapper;
import com.user.returnsassistant.pojo.AfterSaleActionRequest;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleApplicationCreateRequest;
import com.user.returnsassistant.pojo.AfterSaleApplicationSearch;
import com.user.returnsassistant.pojo.AfterSaleDiagnosis;
import com.user.returnsassistant.pojo.AfterSaleEvidence;
import com.user.returnsassistant.pojo.AfterSaleEvidenceRequest;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleApplicationService;
import com.user.returnsassistant.service.AfterSaleDiagnosisService;
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
    private static final Set<String> REVIEWABLE_STATUSES = Set.of("SUBMITTED", "UNDER_REVIEW", "NEED_MORE_EVIDENCE");
    private static final Set<String> TERMINAL_STATUSES = Set.of("REJECTED", "COMPLETED", "CANCELLED");

    @Autowired
    private AfterSaleApplicationMapper applicationMapper;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;
    @Autowired
    private AfterSaleEvidenceMapper evidenceMapper;
    @Autowired
    private DemoOrderMapper orderMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ServiceTicketMapper ticketMapper;
    @Autowired
    private AfterSaleDiagnosisService diagnosisService;

    @Override
    public PageResult<AfterSaleApplication> page(AfterSaleApplicationSearch search) {
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<AfterSaleApplication> page = (Page<AfterSaleApplication>) applicationMapper.page(search);
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public AfterSaleApplication getById(Long id) {
        AfterSaleApplication application = applicationMapper.getById(id);
        if (application == null) {
            throw new BusinessException("售后申请不存在");
        }
        application.setProcessLogs(processLogMapper.listByApplicationId(id));
        application.setEvidences(evidenceMapper.listByApplicationId(id));
        if (application.getDiagnosisId() != null) {
            application.setDiagnosis(diagnosisService.getInternal(application.getDiagnosisId()));
        }
        hydrateCustomerResult(application);
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
        AfterSaleDiagnosis diagnosis = diagnosisService.getOwnedForBinding(request.getDiagnosisId(), order.getId(), customer);

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
        if (diagnosis != null) {
            diagnosisService.bindApplication(diagnosis.getId(), application.getId());
            applicationMapper.bindDiagnosis(application.getId(), diagnosis.getId());
        }
        writeLog(application.getId(), customer, "SUBMIT", null, "SUBMITTED", "顾客提交售后申请：" + application.getReasonText());
        if (diagnosis != null) {
            writeLog(application.getId(), customer, "SYSTEM_MARK", "SUBMITTED", "SUBMITTED",
                    "已绑定前置诊断：" + diagnosis.getDiagnosisNo() + "，建议 "
                            + diagnosis.getSuggestedServiceType() + " / " + diagnosis.getDecisionLevel());
        }
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

    @Transactional
    @Override
    public AfterSaleApplication complete(Long id, AfterSaleActionRequest request, UserAccount admin) {
        AfterSaleApplication application = getById(id);
        if (TERMINAL_STATUSES.contains(application.getStatus())) {
            throw new BusinessException("当前售后申请已经结束");
        }
        if (REVIEWABLE_STATUSES.contains(application.getStatus())) {
            throw new BusinessException("请先完成审核或补材料流程，再确认完成");
        }
        AfterSaleApplication update = new AfterSaleApplication();
        update.setId(id);
        update.setStatus("COMPLETED");
        update.setApprovedAmount(application.getApprovedAmount());
        update.setAssignedTo(application.getAssignedTo());
        update.setClosedAt(LocalDateTime.now());
        applicationMapper.updateDecision(update);
        writeLog(id, admin, "CONFIRM", application.getStatus(), "COMPLETED", cleanRemark(request, "管理员确认售后处理完成，顾客可进行服务评价。"));
        orderMapper.updateAfterSaleStatus(application.getOrderId(), "FINISHED");
        return getById(id);
    }

    @Transactional
    @Override
    public AfterSaleApplication requestEvidence(Long id, AfterSaleActionRequest request, UserAccount admin) {
        AfterSaleApplication application = getById(id);
        ensureReviewable(application);
        String remark = cleanRequired(request == null ? null : request.getRemark(), "要求补充材料必须填写说明");

        AfterSaleApplication update = new AfterSaleApplication();
        update.setId(id);
        update.setStatus("NEED_MORE_EVIDENCE");
        update.setApprovedAmount(application.getApprovedAmount());
        update.setAssignedTo(request == null ? null : request.getAssignedTo());
        applicationMapper.updateDecision(update);
        writeLog(id, admin, "REQUEST_MORE_EVIDENCE", application.getStatus(), "NEED_MORE_EVIDENCE", remark);
        orderMapper.updateAfterSaleStatus(application.getOrderId(), toOrderAfterSaleStatus(application.getServiceType(), "NEED_MORE_EVIDENCE"));
        return getById(id);
    }

    @Transactional
    @Override
    public AfterSaleEvidence addEvidence(Long id, AfterSaleEvidenceRequest request, UserAccount customer) {
        AfterSaleApplication application = getById(id);
        if (!"ADMIN".equals(customer.getRole()) && !Objects.equals(application.getUserId(), customer.getId())) {
            throw new BusinessException("只能为自己的售后申请补充凭证");
        }
        if (TERMINAL_STATUSES.contains(application.getStatus())) {
            throw new BusinessException("当前售后申请已结束，不能补充凭证");
        }
        AfterSaleEvidence evidence = new AfterSaleEvidence();
        evidence.setApplicationId(id);
        evidence.setEvidenceType(normalizeEvidenceType(request == null ? null : request.getEvidenceType()));
        evidence.setFileUrl(hasText(request == null ? null : request.getFileUrl()) ? request.getFileUrl().trim() : null);
        evidence.setContent(cleanRequired(request == null ? null : request.getContent(), "请填写凭证内容"));
        evidence.setUploadedBy(customer.getId());
        evidenceMapper.insert(evidence);
        writeLog(id, customer, "SUPPLEMENT_EVIDENCE", application.getStatus(), application.getStatus(), "补充凭证：" + evidence.getContent());
        return evidence;
    }

    @Transactional
    @Override
    public ServiceTicket createTicket(Long id, AfterSaleActionRequest request, UserAccount admin) {
        AfterSaleApplication application = getById(id);
        if (application.getTicketId() != null) {
            ServiceTicket existing = ticketMapper.getById(application.getTicketId());
            if (existing != null) {
                return existing;
            }
        }
        ChatSession session = new ChatSession();
        session.setSessionNo(NoUtils.sessionNo());
        session.setUserId(application.getUserId());
        session.setOrderId(application.getOrderId());
        session.setTitle("售后申请转人工工单：" + application.getApplicationNo());
        session.setChannel("ADMIN_TEST");
        session.setStatus("ACTIVE");
        session.setCurrentIntent("COMPLAINT_TRANSFER");
        session.setSummary("由真实售后申请创建的工单：" + application.getReasonText());
        sessionMapper.insert(session);

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketNo(NoUtils.ticketNo());
        ticket.setSessionId(session.getId());
        ticket.setOrderId(application.getOrderId());
        ticket.setUserId(application.getUserId());
        ticket.setIntentCode("COMPLAINT_TRANSFER");
        ticket.setPriority(application.getPriority());
        ticket.setStatus("PENDING");
        ticket.setCustomerIssue(application.getReasonText());
        ticket.setAiSummary(application.getAiSummary());
        ticket.setSuggestedAction(cleanRemark(request, "请客服结合售后申请、凭证和处理记录继续跟进。"));
        ticket.setAssignedTo(admin.getDisplayName());
        ticketMapper.insert(ticket);
        applicationMapper.bindTicket(id, ticket.getId());
        writeLog(id, admin, "CREATE_TICKET", application.getStatus(), application.getStatus(), "创建关联工单：" + ticket.getTicketNo());
        return ticketMapper.getById(ticket.getId());
    }

    @Transactional
    @Override
    public void appendTicketProcessLog(Long ticketId, String action, String remark, UserAccount admin) {
        AfterSaleApplication application = applicationMapper.getByTicketId(ticketId);
        if (application == null) {
            return;
        }
        writeLog(application.getId(), admin, action, application.getStatus(), application.getStatus(), remark);
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

    private String normalizeEvidenceType(String evidenceType) {
        String value = hasText(evidenceType) ? evidenceType.trim().toUpperCase() : "TEXT";
        if (!Set.of("IMAGE", "VIDEO", "TEXT", "LOGISTICS_NO").contains(value)) {
            throw new BusinessException("凭证类型不正确");
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

    private void hydrateCustomerResult(AfterSaleApplication application) {
        if (application == null) {
            return;
        }
        List<AfterSaleProcessLog> logs = application.getProcessLogs() == null ? List.of() : application.getProcessLogs();
        AfterSaleProcessLog finalDecision = latestLog(logs, "CONFIRM", "REJECT", "APPROVE", "REQUEST_MORE_EVIDENCE");
        AfterSaleProcessLog finalReply = latestLog(logs, "USE_REPLY_DRAFT");
        application.setCustomerResultSummary(buildCustomerResultSummary(application, finalDecision));
        application.setCustomerFinalReply(finalReply == null ? null : extractCustomerReply(finalReply.getRemark()));
        application.setCustomerNextAction(buildCustomerNextAction(application));
    }

    private AfterSaleProcessLog latestLog(List<AfterSaleProcessLog> logs, String... actions) {
        Set<String> actionSet = Set.of(actions);
        for (int i = logs.size() - 1; i >= 0; i--) {
            AfterSaleProcessLog log = logs.get(i);
            if (actionSet.contains(log.getAction())) {
                return log;
            }
        }
        return null;
    }

    private String extractCustomerReply(String remark) {
        if (!hasText(remark)) {
            return null;
        }
        String marker = "摘要：";
        int markerIndex = remark.indexOf(marker);
        if (markerIndex >= 0 && markerIndex + marker.length() < remark.length()) {
            return remark.substring(markerIndex + marker.length()).trim();
        }
        return remark.trim();
    }

    private String buildCustomerResultSummary(AfterSaleApplication application, AfterSaleProcessLog finalDecision) {
        String amountText = application.getApprovedAmount() == null
                ? "金额以处理记录为准"
                : "审核金额 ¥" + application.getApprovedAmount().stripTrailingZeros().toPlainString();
        if ("COMPLETED".equals(application.getStatus())) {
            String remark = finalDecision == null || !hasText(finalDecision.getRemark())
                    ? "管理员确认售后处理完成"
                    : finalDecision.getRemark();
            return "处理完成：" + amountText + "。客服说明：" + remark;
        }
        if ("REJECTED".equals(application.getStatus())) {
            String remark = finalDecision == null || !hasText(finalDecision.getRemark())
                    ? "申请未通过审核"
                    : finalDecision.getRemark();
            return "申请已驳回。原因：" + remark;
        }
        if ("NEED_MORE_EVIDENCE".equals(application.getStatus())) {
            String remark = finalDecision == null || !hasText(finalDecision.getRemark())
                    ? "请补充商品问题、物流或其他说明"
                    : finalDecision.getRemark();
            return "等待补充材料：" + remark;
        }
        if ("WAIT_BUYER_SEND".equals(application.getStatus()) || "REFUNDING".equals(application.getStatus()) || "EXCHANGING".equals(application.getStatus())) {
            return "审核通过：" + amountText + "。请按下一步提示继续处理。";
        }
        return "当前申请仍在处理中，请关注时间线和下一步提示。";
    }

    private String buildCustomerNextAction(AfterSaleApplication application) {
        return switch (application.getStatus()) {
            case "SUBMITTED", "UNDER_REVIEW" -> "等待管理员审核，期间可继续补充凭证。";
            case "NEED_MORE_EVIDENCE" -> "请补充物流单号、图片链接或文字说明，提交后客服会复核。";
            case "WAIT_BUYER_SEND" -> "请按客服要求寄回商品，并在凭证材料中填写物流单号。";
            case "WAIT_SELLER_RECEIVE" -> "等待商家确认收货。";
            case "REFUNDING" -> "等待退款处理完成，如超时可在线咨询。";
            case "EXCHANGING" -> "等待换货商品发出，如地址有变请及时联系。";
            case "REJECTED" -> "查看驳回原因，必要时整理新凭证后重新申请。";
            case "COMPLETED" -> "处理已完成，可提交服务评价并保留处理记录。";
            case "CANCELLED" -> "申请已取消，无需继续处理。";
            default -> "查看时间线并等待客服下一步通知。";
        };
    }
}
