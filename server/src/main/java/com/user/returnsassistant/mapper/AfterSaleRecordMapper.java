package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.AfterSaleRecord;
import com.user.returnsassistant.pojo.AfterSaleRecordSearch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AfterSaleRecordMapper {
    @Select("""
            <script>
            select count(*) from after_sale_record r
            where 1=1
            <if test="s.orderId != null">and r.order_id=#{s.orderId}</if>
            <if test="s.status != null and s.status != ''">and r.status=#{s.status}</if>
            <if test="s.serviceType != null and s.serviceType != ''">and r.service_type=#{s.serviceType}</if>
            </script>
            """)
    long count(@Param("s") AfterSaleRecordSearch search);

    @Select("""
            <script>
            select r.*, o.order_no
            from after_sale_record r
            left join demo_order o on r.order_id=o.id
            where 1=1
            <if test="s.orderId != null">and r.order_id=#{s.orderId}</if>
            <if test="s.status != null and s.status != ''">and r.status=#{s.status}</if>
            <if test="s.serviceType != null and s.serviceType != ''">and r.service_type=#{s.serviceType}</if>
            order by r.created_at desc, r.id desc
            limit #{s.offset}, #{s.limit}
            </script>
            """)
    List<AfterSaleRecord> page(@Param("s") AfterSaleRecordSearch search);

    @Select("select r.*, o.order_no from after_sale_record r left join demo_order o on r.order_id=o.id where r.id=#{id}")
    AfterSaleRecord getById(Long id);

    @Select("select r.*, o.order_no from after_sale_record r left join demo_order o on r.order_id=o.id where r.order_id=#{orderId} order by r.created_at desc")
    List<AfterSaleRecord> listByOrderId(Long orderId);

    @Insert("""
            insert into after_sale_record(after_sale_no, order_id, service_type, reason, status, refund_amount, apply_at, handle_at, remark)
            values(#{afterSaleNo}, #{orderId}, #{serviceType}, #{reason}, coalesce(#{status}, 'APPLIED'), #{refundAmount}, coalesce(#{applyAt}, now()), #{handleAt}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AfterSaleRecord record);

    @Update("""
            update after_sale_record
            set service_type=#{serviceType}, reason=#{reason}, status=#{status}, refund_amount=#{refundAmount},
                apply_at=#{applyAt}, handle_at=#{handleAt}, remark=#{remark}
            where id=#{id}
            """)
    void update(AfterSaleRecord record);

    @Delete("delete from after_sale_record where id=#{id}")
    void delete(Long id);
}
