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
        return docMapper.search(query, intentCode, actualLimit);
    }
}
