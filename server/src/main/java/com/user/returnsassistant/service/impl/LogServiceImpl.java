package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.mapper.AiCallLogMapper;
import com.user.returnsassistant.mapper.RetrievalLogMapper;
import com.user.returnsassistant.pojo.AiCallLog;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.RetrievalLog;
import com.user.returnsassistant.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl implements LogService {
    @Autowired
    private AiCallLogMapper aiCallLogMapper;
    @Autowired
    private RetrievalLogMapper retrievalLogMapper;

    @Override
    public PageResult<AiCallLog> pageAiLogs(Integer page, Integer pageSize, String status) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return new PageResult<>(aiCallLogMapper.count(status), aiCallLogMapper.page(status, (currentPage - 1) * size, size));
    }

    @Override
    public PageResult<RetrievalLog> pageRetrievalLogs(Integer page, Integer pageSize, String keyword) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        return new PageResult<>(retrievalLogMapper.count(keyword), retrievalLogMapper.page(keyword, (currentPage - 1) * size, size));
    }
}
