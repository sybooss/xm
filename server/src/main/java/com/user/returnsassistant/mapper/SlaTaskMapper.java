package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.SlaTask;
import com.user.returnsassistant.pojo.SlaTaskSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SlaTaskMapper {
    long count(@Param("s") SlaTaskSearch search);

    List<SlaTask> page(@Param("s") SlaTaskSearch search);
}
