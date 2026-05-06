package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ReplyDraft;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReplyDraftMapper {
    List<ReplyDraft> listByApplicationId(@Param("applicationId") Long applicationId);

    ReplyDraft getById(@Param("id") Long id);

    void insert(ReplyDraft draft);

    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("auditRemark") String auditRemark,
                      @Param("used") boolean used);
}
