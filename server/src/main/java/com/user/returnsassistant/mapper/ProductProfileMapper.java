package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ProductProfile;
import com.user.returnsassistant.pojo.ProductProfileSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductProfileMapper {
    long count(@Param("s") ProductProfileSearch search);

    List<ProductProfile> page(@Param("s") ProductProfileSearch search);

    ProductProfile getById(Long id);

    ProductProfile getByProductName(String productName);

    ProductProfile findBestMatch(@Param("productName") String productName);

    List<ProductProfile> searchEnabled(@Param("keyword") String keyword, @Param("limit") Integer limit);

    void insert(ProductProfile profile);

    void update(ProductProfile profile);
}
