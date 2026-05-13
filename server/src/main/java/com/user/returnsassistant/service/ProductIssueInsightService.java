package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.ProductIssueAlert;
import com.user.returnsassistant.pojo.ProductIssueInsightSearch;
import com.user.returnsassistant.pojo.ProductIssueInsightSummary;
import com.user.returnsassistant.pojo.ProductIssueRefreshRequest;
import com.user.returnsassistant.pojo.ProductIssueRefreshResult;
import com.user.returnsassistant.pojo.UserAccount;

import java.util.List;

public interface ProductIssueInsightService {
    ProductIssueRefreshResult refresh(ProductIssueRefreshRequest request, UserAccount admin);

    PageResult<ProductIssueAlert> page(ProductIssueInsightSearch search);

    ProductIssueInsightSummary summary(Integer days);

    List<ProductIssueAlert> listOpenByProduct(String productName, Integer days);
}
