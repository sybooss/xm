package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.KnowledgeDoc;
import com.user.returnsassistant.pojo.KnowledgeDocSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KnowledgeDocMapper {
    long count(@Param("s") KnowledgeDocSearch search);

    List<KnowledgeDoc> page(@Param("s") KnowledgeDocSearch search);

    KnowledgeDoc getById(Long id);

    void insert(KnowledgeDoc doc);

    void update(KnowledgeDoc doc);

    void softDelete(Long id);

    List<KnowledgeDoc> search(@Param("query") String query, @Param("intentCode") String intentCode, @Param("limit") Integer limit);
}
