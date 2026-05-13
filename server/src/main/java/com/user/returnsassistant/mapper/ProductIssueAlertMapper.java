package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ProductIssueAlert;
import com.user.returnsassistant.pojo.ProductIssueInsightSearch;
import com.user.returnsassistant.pojo.ProductIssueSourceItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductIssueAlertMapper {
    List<ProductIssueSourceItem> listSourceItems(@Param("days") Integer days);

    ProductIssueAlert getOpenByUniqueKey(@Param("productName") String productName,
                                         @Param("issueKeyword") String issueKeyword,
                                         @Param("timeWindowDays") Integer timeWindowDays);

    List<ProductIssueAlert> listOpenByProduct(@Param("productName") String productName,
                                              @Param("days") Integer days);

    ProductIssueAlert getTop(@Param("days") Integer days, @Param("status") String status);

    Long countByLevel(@Param("days") Integer days,
                      @Param("status") String status,
                      @Param("alertLevel") String alertLevel);

    Long countDistinctProducts(@Param("days") Integer days, @Param("status") String status);

    Long sumApplications(@Param("days") Integer days, @Param("status") String status);

    List<ProductIssueAlert> page(@Param("s") ProductIssueInsightSearch search);

    void insert(ProductIssueAlert alert);

    void update(ProductIssueAlert alert);
}
