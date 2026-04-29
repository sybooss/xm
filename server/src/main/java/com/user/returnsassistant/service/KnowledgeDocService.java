package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.KnowledgeDocSearch;
import com.user.returnsassistant.pojo.PageResult;

import java.util.List;

public interface KnowledgeDocService {
    PageResult<KnowledgeDoc> page(KnowledgeDocSearch search);

    KnowledgeDoc getById(Long id);

    void save(KnowledgeDoc doc);

    void update(Long id, KnowledgeDoc doc);

    void delete(Long id);

    List<KnowledgeDoc> search(String query, String intentCode, Integer limit);
}
