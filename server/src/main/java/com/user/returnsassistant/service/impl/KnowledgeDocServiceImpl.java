package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.KnowledgeDocMapper;
import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.KnowledgeDocSearch;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.service.KnowledgeDocService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeDocServiceImpl implements KnowledgeDocService {
    @Autowired
    private KnowledgeDocMapper docMapper;

    @Override
    public PageResult<KnowledgeDoc> page(KnowledgeDocSearch search) {
        return new PageResult<>(docMapper.count(search), docMapper.page(search));
    }

    @Override
    public KnowledgeDoc getById(Long id) {
        KnowledgeDoc doc = docMapper.getById(id);
        if (doc == null) {
            throw new BusinessException("知识文档不存在");
        }
        return doc;
    }

    @Override
    public void save(KnowledgeDoc doc) {
        docMapper.insert(doc);
    }

    @Override
    public void update(Long id, KnowledgeDoc doc) {
        doc.setId(id);
        docMapper.update(doc);
    }

    @Override
    public void delete(Long id) {
        docMapper.softDelete(id);
    }

    @Override
    public List<KnowledgeDoc> search(String query, String intentCode, Integer limit) {
        int actualLimit = limit == null || limit < 1 ? 5 : Math.min(limit, 20);
        List<KnowledgeDoc> docs = docMapper.search(query, intentCode, actualLimit);
        String inferredIntent = inferIntent(query);
        boolean inferred = false;
        if (docs.isEmpty() && !hasText(intentCode) && hasText(inferredIntent)) {
            docs = docMapper.search(query, inferredIntent, actualLimit);
            inferred = true;
        }
        enrichHits(docs, query, inferred ? inferredIntent : intentCode, inferred);
        return docs;
    }

    private void enrichHits(List<KnowledgeDoc> docs, String query, String intentCode, boolean inferred) {
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeDoc doc = docs.get(i);
            doc.setRankNo(i + 1);
            double priorityScore = Math.min(0.12, Math.max(0, nullToZero(doc.getPriority())) / 1000.0);
            doc.setScore(Math.max(0.5, 0.94 - i * 0.05 + priorityScore));
            doc.setHitReason(buildHitReason(doc, query, intentCode, inferred));
            if (!hasText(doc.getContentPreview())) {
                doc.setContentPreview(preview(doc.getContent()));
            }
        }
    }

    private String buildHitReason(KnowledgeDoc doc, String query, String intentCode, boolean inferred) {
        String text = nullToEmpty(query);
        StringBuilder reason = new StringBuilder();
        if (hasText(doc.getTitle()) && text.contains(doc.getTitle())) {
            reason.append("问题包含文档标题");
        } else if (hasText(doc.getQuestion()) && doc.getQuestion().contains(text)) {
            reason.append("常见问法包含用户问题");
        } else if (hasMatchedKeyword(text, doc.getKeywords())) {
            reason.append("命中文档关键词");
        } else if (hasText(intentCode) && intentCode.equals(doc.getIntentCode())) {
            reason.append(inferred ? "整句未命中，按售后关键词推断意图召回" : "按指定意图召回");
        } else {
            reason.append("命中标题、问题、正文或关键词");
        }
        if (hasText(doc.getIntentCode())) {
            reason.append("，意图=").append(doc.getIntentCode());
        }
        reason.append("，优先级=").append(nullToZero(doc.getPriority()));
        return reason.toString();
    }

    private boolean hasMatchedKeyword(String query, String keywords) {
        if (!hasText(query) || !hasText(keywords)) {
            return false;
        }
        for (String keyword : keywords.split("[,，、\\s]+")) {
            if (hasText(keyword) && query.contains(keyword.trim())) {
                return true;
            }
        }
        return false;
    }

    private String inferIntent(String query) {
        String text = nullToEmpty(query);
        if (containsAny(text, "投诉", "人工", "介入", "不处理")) {
            return "COMPLAINT_TRANSFER";
        }
        if (containsAny(text, "换货", "换新", "规格不符")) {
            return "EXCHANGE_APPLY";
        }
        if (containsAny(text, "退款", "到账", "钱", "退回", "原路")) {
            return "REFUND_PROGRESS";
        }
        if (containsAny(text, "物流", "快递", "包裹", "不更新", "丢")) {
            return "LOGISTICS_QUERY";
        }
        if (containsAny(text, "退货", "七天", "寄回", "退掉")) {
            return "RETURN_APPLY";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String preview(String content) {
        String text = nullToEmpty(content);
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
