package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleProcessLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AfterSaleProcessLogMapper {
    List<AfterSaleProcessLog> listByApplicationId(@Param("applicationId") Long applicationId);

    void insert(AfterSaleProcessLog log);
}
