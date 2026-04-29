package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.KnowledgeCategory;

import java.util.List;

public interface KnowledgeCategoryService {
    List<KnowledgeCategory> list(Integer enabled);

    KnowledgeCategory getById(Long id);

    void save(KnowledgeCategory category);

    void update(Long id, KnowledgeCategory category);

    void delete(Long id);
}
