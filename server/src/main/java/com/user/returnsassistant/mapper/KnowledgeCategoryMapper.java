package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.KnowledgeCategory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeCategoryMapper {
    List<KnowledgeCategory> list(@Param("enabled") Integer enabled);

    KnowledgeCategory getById(Long id);

    long countDocs(Long id);

    void deleteSoftDeletedDocs(Long id);

    void insert(KnowledgeCategory category);

    void update(KnowledgeCategory category);

    void delete(Long id);
}
