package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.ServiceTicket;
import com.user.returnsassistant.pojo.ServiceTicketSearch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ServiceTicketMapper {
    @Select("""
            <script>
            select count(*)
            from service_ticket t
            left join chat_session s on t.session_id=s.id
            left join demo_order o on t.order_id=o.id
            where t.deleted=0
            <if test="s.sessionId != null">and t.session_id=#{s.sessionId}</if>
            <if test="s.orderId != null">and t.order_id=#{s.orderId}</if>
            <if test="s.status != null and s.status != ''">and t.status=#{s.status}</if>
            <if test="s.priority != null and s.priority != ''">and t.priority=#{s.priority}</if>
            <if test="s.intentCode != null and s.intentCode != ''">and t.intent_code=#{s.intentCode}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (t.ticket_no like concat('%',#{s.keyword},'%')
                 or t.customer_issue like concat('%',#{s.keyword},'%')
                 or t.ai_summary like concat('%',#{s.keyword},'%')
                 or o.order_no like concat('%',#{s.keyword},'%'))
            </if>
            </script>
            """)
    long count(@Param("s") ServiceTicketSearch search);

    @Select("""
            <script>
            select t.*, s.session_no, o.order_no
            from service_ticket t
            left join chat_session s on t.session_id=s.id
            left join demo_order o on t.order_id=o.id
            where t.deleted=0
            <if test="s.sessionId != null">and t.session_id=#{s.sessionId}</if>
            <if test="s.orderId != null">and t.order_id=#{s.orderId}</if>
            <if test="s.status != null and s.status != ''">and t.status=#{s.status}</if>
            <if test="s.priority != null and s.priority != ''">and t.priority=#{s.priority}</if>
            <if test="s.intentCode != null and s.intentCode != ''">and t.intent_code=#{s.intentCode}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (t.ticket_no like concat('%',#{s.keyword},'%')
                 or t.customer_issue like concat('%',#{s.keyword},'%')
                 or t.ai_summary like concat('%',#{s.keyword},'%')
                 or o.order_no like concat('%',#{s.keyword},'%'))
            </if>
            order by t.created_at desc, t.id desc
            limit #{s.offset}, #{s.limit}
            </script>
            """)
    List<ServiceTicket> page(@Param("s") ServiceTicketSearch search);

    @Select("""
            select t.*, s.session_no, o.order_no
            from service_ticket t
            left join chat_session s on t.session_id=s.id
            left join demo_order o on t.order_id=o.id
            where t.id=#{id} and t.deleted=0
            """)
    ServiceTicket getById(Long id);

    @Select("""
            select t.*, s.session_no, o.order_no
            from service_ticket t
            left join chat_session s on t.session_id=s.id
            left join demo_order o on t.order_id=o.id
            where t.session_id=#{sessionId} and t.deleted=0
            order by t.created_at desc, t.id desc
            """)
    List<ServiceTicket> listBySessionId(Long sessionId);

    @Select("""
            select t.*, s.session_no, o.order_no
            from service_ticket t
            left join chat_session s on t.session_id=s.id
            left join demo_order o on t.order_id=o.id
            where t.session_id=#{sessionId}
              and t.deleted=0
              and t.status in ('PENDING', 'PROCESSING')
            order by t.created_at desc, t.id desc
            limit 1
            """)
    ServiceTicket getOpenBySessionId(Long sessionId);

    @Insert("""
            insert into service_ticket(ticket_no, session_id, message_id, order_id, user_id, intent_code, priority, status,
                                       customer_issue, ai_summary, suggested_action, assigned_to)
            values(#{ticketNo}, #{sessionId}, #{messageId}, #{orderId}, #{userId}, #{intentCode},
                   coalesce(#{priority}, 'NORMAL'), coalesce(#{status}, 'PENDING'),
                   #{customerIssue}, #{aiSummary}, #{suggestedAction}, #{assignedTo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ServiceTicket ticket);

    @Update("""
            update service_ticket
            set priority=#{priority}, status=#{status}, customer_issue=#{customerIssue}, ai_summary=#{aiSummary},
                suggested_action=#{suggestedAction}, assigned_to=#{assignedTo},
                resolved_at=case when #{status} in ('RESOLVED', 'CLOSED') then coalesce(#{resolvedAt}, now()) else #{resolvedAt} end
            where id=#{id} and deleted=0
            """)
    void update(ServiceTicket ticket);

    @Update("update service_ticket set deleted=1 where id=#{id}")
    void delete(Long id);
}
