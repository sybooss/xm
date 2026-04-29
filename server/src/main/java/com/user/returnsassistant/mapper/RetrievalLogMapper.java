package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.RetrievalLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RetrievalLogMapper {
    @Insert("""
            insert into retrieval_log(session_id, message_id, query_text, doc_id, rank_no, score, hit_reason, doc_title_snapshot, doc_content_snapshot)
            values(#{sessionId}, #{messageId}, #{queryText}, #{docId}, #{rankNo}, #{score}, #{hitReason}, #{docTitleSnapshot}, #{docContentSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RetrievalLog log);

    @Select("select * from retrieval_log where session_id=#{sessionId} order by created_at desc, rank_no asc")
    List<RetrievalLog> listBySessionId(Long sessionId);

    @Select("""
            <script>
            select count(*) from retrieval_log
            where 1=1
            <if test="keyword != null and keyword != ''">
              and (query_text like concat('%',#{keyword},'%') or doc_title_snapshot like concat('%',#{keyword},'%'))
            </if>
            </script>
            """)
    long count(@Param("keyword") String keyword);

    @Select("""
            <script>
            select * from retrieval_log
            where 1=1
            <if test="keyword != null and keyword != ''">
              and (query_text like concat('%',#{keyword},'%') or doc_title_snapshot like concat('%',#{keyword},'%'))
            </if>
            order by created_at desc, id desc
            limit #{offset}, #{limit}
            </script>
            """)
    List<RetrievalLog> page(@Param("keyword") String keyword, @Param("offset") Integer offset, @Param("limit") Integer limit);
}
