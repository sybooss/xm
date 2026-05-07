package com.user.returnsassistant.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.user.returnsassistant.mapper.SlaTaskMapper;
import com.user.returnsassistant.pojo.PageResult;
import com.user.returnsassistant.pojo.SlaTask;
import com.user.returnsassistant.pojo.SlaTaskSearch;
import com.user.returnsassistant.service.SlaTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SlaTaskServiceImpl implements SlaTaskService {
    @Autowired
    private SlaTaskMapper slaTaskMapper;

    @Override
    public PageResult<SlaTask> page(SlaTaskSearch search) {
        PageHelper.startPage(search.getPage(), search.getPageSize());
        Page<SlaTask> page = (Page<SlaTask>) slaTaskMapper.page(search);
        List<SlaTask> rows = page.getResult();
        LocalDateTime now = LocalDateTime.now();
        for (SlaTask task : rows) {
            enrich(task, now);
        }
        return new PageResult<>(page.getTotal(), rows);
    }

    private void enrich(SlaTask task, LocalDateTime now) {
        if (task.getSlaDeadline() == null) {
            task.setRiskLabel("未设置 SLA");
            task.setRemainingHours(null);
            return;
        }
        long hours = Duration.between(now, task.getSlaDeadline()).toHours();
        task.setRemainingHours(hours);
        if (hours < 0) {
            task.setRiskLabel("已超时");
        } else if (hours <= 24) {
            task.setRiskLabel("即将超时");
        } else if ("URGENT".equals(task.getPriority()) || "HIGH".equals(task.getPriority())) {
            task.setRiskLabel("高优先级");
        } else if ("NEED_MORE_EVIDENCE".equals(task.getStatus())) {
            task.setRiskLabel("待顾客补材料");
        } else {
            task.setRiskLabel("正常跟进");
        }
    }
}
