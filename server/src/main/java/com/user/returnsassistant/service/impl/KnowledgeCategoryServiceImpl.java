package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.KnowledgeCategoryMapper;
import com.user.returnsassistant.pojo.KnowledgeCategory;
import com.user.returnsassistant.service.KnowledgeCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeCategoryServiceImpl implements KnowledgeCategoryService {
    @Autowired
    private KnowledgeCategoryMapper categoryMapper;

    @Override
    public List<KnowledgeCategory> list(Integer enabled) {
        return categoryMapper.list(enabled);
    }

    @Override
    public KnowledgeCategory getById(Long id) {
        KnowledgeCategory category = categoryMapper.getById(id);
        if (category == null) {
            throw new BusinessException("知识分类不存在");
        }
        return category;
    }

    @Override
    public void save(KnowledgeCategory category) {
        categoryMapper.insert(category);
    }

    @Override
    public void update(Long id, KnowledgeCategory category) {
        category.setId(id);
        categoryMapper.update(category);
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.countDocs(id) > 0) {
            throw new BusinessException("分类下存在知识文档，不能删除");
        }
        categoryMapper.deleteSoftDeletedDocs(id);
        categoryMapper.delete(id);
    }
}
