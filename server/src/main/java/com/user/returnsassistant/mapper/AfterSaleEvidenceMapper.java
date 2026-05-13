package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleEvidence;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AfterSaleEvidenceMapper {
    AfterSaleEvidence getById(@Param("id") Long id);

    List<AfterSaleEvidence> listByApplicationId(@Param("applicationId") Long applicationId);

    void insert(AfterSaleEvidence evidence);
}
