package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.SlaTask;
import com.user.returnsassistant.pojo.SlaTaskSearch;

public interface SlaTaskService {
    PageResult<SlaTask> page(SlaTaskSearch search);
}
