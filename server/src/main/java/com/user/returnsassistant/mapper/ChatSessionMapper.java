package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ChatSession;
import com.user.returnsassistant.pojo.ChatSessionSearch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    @Select("""
            <script>
            select count(*)
            from chat_session s
            left join demo_order o on s.order_id=o.id
            where 1=1
            <if test="s.status != null and s.status != ''">and s.status=#{s.status}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (s.title like concat('%',#{s.keyword},'%')
                 or s.current_intent like concat('%',#{s.keyword},'%')
                 or o.order_no like concat('%',#{s.keyword},'%'))
            </if>
            </script>
            """)
    long count(@Param("s") ChatSessionSearch search);

    @Select("""
            <script>
            select s.*, o.order_no
            from chat_session s
            left join demo_order o on s.order_id=o.id
            where 1=1
            <if test="s.status != null and s.status != ''">and s.status=#{s.status}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (s.title like concat('%',#{s.keyword},'%')
                 or s.current_intent like concat('%',#{s.keyword},'%')
                 or o.order_no like concat('%',#{s.keyword},'%'))
            </if>
            order by s.updated_at desc, s.id desc
            limit #{s.offset}, #{s.limit}
            </script>
            """)
    List<ChatSession> page(@Param("s") ChatSessionSearch search);

    @Select("select s.*, o.order_no from chat_session s left join demo_order o on s.order_id=o.id where s.id=#{id}")
    ChatSession getById(Long id);

    @Insert("""
            insert into chat_session(session_no, user_id, order_id, title, channel, status, current_intent, summary)
            values(#{sessionNo}, #{userId}, #{orderId}, #{title}, coalesce(#{channel}, 'WEB'), coalesce(#{status}, 'ACTIVE'), #{currentIntent}, #{summary})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatSession session);

    @Update("""
            update chat_session
            set user_id=#{userId}, order_id=#{orderId}, title=#{title}, channel=coalesce(#{channel}, 'WEB'),
                status=coalesce(#{status}, 'ACTIVE'), current_intent=#{currentIntent}, summary=#{summary}
            where id=#{id}
            """)
    void update(ChatSession session);

    @Update("update chat_session set status='CLOSED', closed_at=now() where id=#{id}")
    void close(Long id);

    @Update("update chat_session set order_id=#{orderId} where id=#{id}")
    void bindOrder(@Param("id") Long id, @Param("orderId") Long orderId);

    @Update("update chat_session set current_intent=#{intentCode}, summary=#{summary}, updated_at=now() where id=#{id}")
    void updateSummary(@Param("id") Long id, @Param("intentCode") String intentCode, @Param("summary") String summary);

    @Delete("delete from chat_session where id=#{id}")
    void delete(Long id);
}
