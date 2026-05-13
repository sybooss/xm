package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProductProfile;
import com.user.returnsassistant.pojo.ProductProfileSearch;

public interface ProductProfileService {
    PageResult<ProductProfile> page(ProductProfileSearch search);

    ProductProfile getById(Long id);

    void save(ProductProfile profile);

    void update(Long id, ProductProfile profile);
}
