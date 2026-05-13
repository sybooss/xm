package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.mapper.ProductProfileMapper;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProductProfile;
import com.user.returnsassistant.pojo.ProductProfileSearch;
import com.user.returnsassistant.service.ProductProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductProfileServiceImpl implements ProductProfileService {
    @Autowired
    private ProductProfileMapper profileMapper;

    @Override
    public PageResult<ProductProfile> page(ProductProfileSearch search) {
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<ProductProfile> page = (Page<ProductProfile>) profileMapper.page(search);
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public ProductProfile getById(Long id) {
        ProductProfile profile = profileMapper.getById(id);
        if (profile == null) {
            throw new BusinessException("商品档案不存在");
        }
        return profile;
    }

    @Override
    public void save(ProductProfile profile) {
        validate(profile);
        if (profile.getEnabled() == null) {
            profile.setEnabled(1);
        }
        profileMapper.insert(profile);
    }

    @Override
    public void update(Long id, ProductProfile profile) {
        getById(id);
        validate(profile);
        profile.setId(id);
        if (profile.getEnabled() == null) {
            profile.setEnabled(1);
        }
        profileMapper.update(profile);
    }

    private void validate(ProductProfile profile) {
        if (profile == null) {
            throw new BusinessException("商品档案不能为空");
        }
        if (!hasText(profile.getProductName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (!hasText(profile.getCategory())) {
            throw new BusinessException("商品品类不能为空");
        }
        if (!hasText(profile.getPositioning())) {
            throw new BusinessException("商品定位不能为空");
        }
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
