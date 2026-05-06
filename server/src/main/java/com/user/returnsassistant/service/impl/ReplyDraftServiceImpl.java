package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.AfterSaleProcessLogMapper;
import com.user.returnsassistant.mapper.ReplyDraftMapper;
import com.user.returnsassistant.pojo.AfterSaleApplication;
import com.user.returnsassistant.pojo.AfterSaleEvidence;
import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.ReplyDraft;
import com.user.returnsassistant.pojo.ReplyDraftActionRequest;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.AfterSaleApplicationService;
import com.user.returnsassistant.service.AiService;
import com.user.returnsassistant.service.KnowledgeDocService;
import com.user.returnsassistant.service.ReplyDraftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReplyDraftServiceImpl implements ReplyDraftService {
    @Autowired
    private ReplyDraftMapper draftMapper;
    @Autowired
    private AfterSaleApplicationService applicationService;
    @Autowired
    private KnowledgeDocService knowledgeDocService;
    @Autowired
    private AiService aiService;
    @Autowired
    private AfterSaleProcessLogMapper processLogMapper;

    @Override
    public List<ReplyDraft> listByApplicationId(Long applicationId) {
        applicationService.getById(applicationId);
        return draftMapper.listByApplicationId(applicationId);
    }

    @Transactional
    @Override
    public ReplyDraft generate(Long applicationId, ReplyDraftActionRequest request, UserAccount admin) {
        AfterSaleApplication application = applicationService.getById(applicationId);
        List<KnowledgeDoc> hits = knowledgeDocService.search(application.getReasonText(), toIntentCode(application.getServiceType()), 3);
        String localDraft = buildTemplateDraft(application, hits);
        String prompt = buildPrompt(application, hits, localDraft);
        AiService.AiResult aiResult = aiService.generate(prompt);
        boolean aiUsable = aiResult.used() && hasText(aiResult.reply());

        ReplyDraft draft = new ReplyDraft();
        draft.setApplicationId(applicationId);
        draft.setTicketId(application.getTicketId());
        draft.setDraftContent(aiUsable ? aiResult.reply().trim() : localDraft);
        draft.setSourceType(aiUsable ? "AI" : "TEMPLATE");
        draft.setStatus("DRAFT");
        draft.setRiskLevel(resolveRisk(application));
        draft.setKnowledgeRefs(knowledgeRefs(hits));
        draft.setAiStatus(aiResult.status());
        draft.setAiProvider(aiResult.provider());
        draft.setAiModelName(aiResult.modelName());
        draft.setAuditRemark(cleanRemark(request, aiUsable ? "AI 生成回复草稿，等待管理员确认。" : fallbackRemark(aiResult)));
        draft.setCreatedBy(admin.getId());
        draftMapper.insert(draft);
        writeLog(application, admin, "GENERATE_REPLY_DRAFT", "生成回复草稿：" + sourceLabel(draft.getSourceType()) + "，风险=" + draft.getRiskLevel());
        return requireDraft(draft.getId());
    }

    @Transactional
    @Override
    public ReplyDraft use(Long applicationId, Long draftId, ReplyDraftActionRequest request, UserAccount admin) {
        AfterSaleApplication application = applicationService.getById(applicationId);
        ReplyDraft draft = requireApplicationDraft(applicationId, draftId);
        ensureDraftOpen(draft);
        String remark = cleanRemark(request, "管理员采纳回复草稿，草稿内容可用于后续客服回复。");
        draftMapper.updateStatus(draftId, "USED", remark, true);
        writeLog(application, admin, "USE_REPLY_DRAFT", remark + " 摘要：" + preview(draft.getDraftContent()));
        return requireDraft(draftId);
    }

    @Transactional
    @Override
    public ReplyDraft discard(Long applicationId, Long draftId, ReplyDraftActionRequest request, UserAccount admin) {
        AfterSaleApplication application = applicationService.getById(applicationId);
        ReplyDraft draft = requireApplicationDraft(applicationId, draftId);
        ensureDraftOpen(draft);
        String remark = cleanRemark(request, "管理员废弃回复草稿，未用于客服回复。");
        draftMapper.updateStatus(draftId, "DISCARDED", remark, false);
        writeLog(application, admin, "DISCARD_REPLY_DRAFT", remark);
        return requireDraft(draftId);
    }

    private ReplyDraft requireApplicationDraft(Long applicationId, Long draftId) {
        ReplyDraft draft = requireDraft(draftId);
        if (!applicationId.equals(draft.getApplicationId())) {
            throw new BusinessException("回复草稿不属于当前售后申请");
        }
        return draft;
    }

    private ReplyDraft requireDraft(Long id) {
        ReplyDraft draft = draftMapper.getById(id);
        if (draft == null) {
            throw new BusinessException("回复草稿不存在");
        }
        return draft;
    }

    private void ensureDraftOpen(ReplyDraft draft) {
        if (!"DRAFT".equals(draft.getStatus())) {
            throw new BusinessException("当前草稿已经处理，不能重复操作");
        }
    }

    private String buildPrompt(AfterSaleApplication application, List<KnowledgeDoc> hits, String localDraft) {
        return """
                你是电商售后客服的 AI 副驾驶，只能生成客服回复草稿，不能决定退款、驳回或修改状态。
                请基于售后单、凭证、处理日志和知识库依据，生成一段给顾客看的中文回复草稿。
                要求：先安抚，再说明当前状态，再列出下一步动作；不要承诺自动退款；不要声称已经改状态。

                售后单：%s
                类型：%s
                状态：%s
                优先级：%s
                风险：%s
                原因：%s
                凭证：%s
                处理日志：%s
                知识库依据：%s

                本地兜底草稿：%s
                """.formatted(
                application.getApplicationNo(),
                application.getServiceType(),
                application.getStatus(),
                application.getPriority(),
                resolveRisk(application),
                application.getReasonText(),
                evidenceSummary(application),
                logSummary(application),
                knowledgeSummary(hits),
                localDraft
        );
    }

    private String buildTemplateDraft(AfterSaleApplication application, List<KnowledgeDoc> hits) {
        StringBuilder draft = new StringBuilder();
        draft.append("您好，您的售后申请 ").append(application.getApplicationNo()).append(" 已收到，我们会继续按售后流程跟进。");
        draft.append("当前状态为“").append(statusText(application.getStatus())).append("”，");
        if ("NEED_MORE_EVIDENCE".equals(application.getStatus())) {
            draft.append("请按页面提示补充商品问题照片、物流单号或其他说明，客服会在收到材料后复核。");
        } else if (application.getTicketNo() != null) {
            draft.append("该申请已关联人工工单 ").append(application.getTicketNo()).append("，客服会结合凭证和处理记录继续跟进。");
        } else {
            draft.append("客服会结合订单、凭证和平台规则进行人工审核。");
        }
        if (!hits.isEmpty()) {
            draft.append("可参考规则：").append(hits.get(0).getTitle()).append("。");
        }
        draft.append("如您还有新的凭证或补充说明，可以继续在售后详情中提交。");
        return draft.toString();
    }

    private String evidenceSummary(AfterSaleApplication application) {
        List<AfterSaleEvidence> evidences = application.getEvidences();
        if (evidences == null || evidences.isEmpty()) {
            return "暂无凭证";
        }
        return evidences.stream()
                .limit(5)
                .map(e -> e.getEvidenceType() + ":" + preview(e.getContent()))
                .collect(Collectors.joining("；"));
    }

    private String logSummary(AfterSaleApplication application) {
        List<AfterSaleProcessLog> logs = application.getProcessLogs();
        if (logs == null || logs.isEmpty()) {
            return "暂无处理日志";
        }
        return logs.stream()
                .limit(8)
                .map(log -> log.getAction() + ":" + preview(log.getRemark()))
                .collect(Collectors.joining("；"));
    }

    private String knowledgeSummary(List<KnowledgeDoc> hits) {
        if (hits.isEmpty()) {
            return "未命中知识库，使用本地模板";
        }
        return hits.stream()
                .map(doc -> doc.getTitle() + ":" + preview(firstText(doc.getAnswer(), doc.getContent(), doc.getContentPreview())))
                .collect(Collectors.joining("；"));
    }

    private String knowledgeRefs(List<KnowledgeDoc> hits) {
        if (hits.isEmpty()) {
            return "本地售后模板";
        }
        return hits.stream()
                .map(doc -> doc.getTitle() + "(" + nullToDash(doc.getIntentCode()) + ")")
                .collect(Collectors.joining("；"));
    }

    private String toIntentCode(String serviceType) {
        return switch (serviceType) {
            case "EXCHANGE" -> "EXCHANGE_APPLY";
            case "REFUND" -> "REFUND_PROGRESS";
            case "COMPLAINT" -> "COMPLAINT_TRANSFER";
            default -> "RETURN_APPLY";
        };
    }

    private String resolveRisk(AfterSaleApplication application) {
        if ("URGENT".equals(application.getPriority()) || "COMPLAINT".equals(application.getServiceType())) {
            return "HIGH";
        }
        if ("HIGH".equals(application.getPriority()) || "HIGH".equals(application.getRiskLevel())) {
            return "HIGH";
        }
        if ("MEDIUM".equals(application.getRiskLevel()) || "NEED_MORE_EVIDENCE".equals(application.getStatus())) {
            return "MEDIUM";
        }
        return "LOW";
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

    private String fallbackRemark(AiService.AiResult aiResult) {
        if ("SKIPPED".equals(aiResult.status())) {
            return "AI 未启用或未配置，已生成本地模板草稿。";
        }
        if ("FAILED".equals(aiResult.status())) {
            return "AI 生成失败，已生成本地模板草稿：" + nullToDash(aiResult.errorMessage());
        }
        return "已生成本地模板草稿。";
    }

    private String cleanRemark(ReplyDraftActionRequest request, String fallback) {
        if (request != null && hasText(request.getRemark())) {
            return request.getRemark().trim();
        }
        return fallback;
    }

    private String statusText(String status) {
        return switch (status) {
            case "SUBMITTED" -> "已提交";
            case "UNDER_REVIEW" -> "审核中";
            case "NEED_MORE_EVIDENCE" -> "待补充材料";
            case "WAIT_BUYER_SEND" -> "待买家寄回";
            case "REFUNDING" -> "退款中";
            case "EXCHANGING" -> "换货中";
            case "REJECTED" -> "已驳回";
            case "COMPLETED" -> "已完成";
            default -> status;
        };
    }

    private String sourceLabel(String sourceType) {
        return "AI".equals(sourceType) ? "AI 副驾驶" : "本地模板";
    }

    private String firstText(String... texts) {
        for (String text : texts) {
            if (hasText(text)) {
                return text;
            }
        }
        return "";
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String value = text.replaceAll("\\s+", " ").trim();
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String nullToDash(String text) {
        return hasText(text) ? text : "-";
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
