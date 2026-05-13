package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleRiskAssessment;
import com.user.returnsassistant.pojo.AfterSaleRiskAssessmentSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AfterSaleRiskAssessmentMapper {
    AfterSaleRiskAssessment getByApplicationId(@Param("applicationId") Long applicationId);

    List<AfterSaleRiskAssessment> page(@Param("s") AfterSaleRiskAssessmentSearch search);

    void insert(AfterSaleRiskAssessment assessment);

    void update(AfterSaleRiskAssessment assessment);
}
