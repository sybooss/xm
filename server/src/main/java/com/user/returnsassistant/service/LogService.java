package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.AiCallLog;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.RetrievalLog;

public interface LogService {
    PageResult<AiCallLog> pageAiLogs(Integer page, Integer pageSize, String status);

    PageResult<RetrievalLog> pageRetrievalLogs(Integer page, Integer pageSize, String keyword);
}
