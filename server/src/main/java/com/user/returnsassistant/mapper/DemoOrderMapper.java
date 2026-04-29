package com.user.returnsassistant.mapper;

import com.user.returnsassistant.pojo.DemoOrder;
import com.user.returnsassistant.pojo.OrderSearch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DemoOrderMapper {
    @Select("""
            <script>
            select count(*) from demo_order
            where 1=1
            <if test="s.orderStatus != null and s.orderStatus != ''">and order_status=#{s.orderStatus}</if>
            <if test="s.logisticsStatus != null and s.logisticsStatus != ''">and logistics_status=#{s.logisticsStatus}</if>
            <if test="s.afterSaleStatus != null and s.afterSaleStatus != ''">and after_sale_status=#{s.afterSaleStatus}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (order_no like concat('%',#{s.keyword},'%') or product_name like concat('%',#{s.keyword},'%'))
            </if>
            </script>
            """)
    long count(@Param("s") OrderSearch search);

    @Select("""
            <script>
            select * from demo_order
            where 1=1
            <if test="s.orderStatus != null and s.orderStatus != ''">and order_status=#{s.orderStatus}</if>
            <if test="s.logisticsStatus != null and s.logisticsStatus != ''">and logistics_status=#{s.logisticsStatus}</if>
            <if test="s.afterSaleStatus != null and s.afterSaleStatus != ''">and after_sale_status=#{s.afterSaleStatus}</if>
            <if test="s.keyword != null and s.keyword != ''">
                and (order_no like concat('%',#{s.keyword},'%') or product_name like concat('%',#{s.keyword},'%'))
            </if>
            order by updated_at desc, id desc
            limit #{s.offset}, #{s.limit}
            </script>
            """)
    List<DemoOrder> page(@Param("s") OrderSearch search);

    @Select("select * from demo_order where id=#{id}")
    DemoOrder getById(Long id);

    @Select("select * from demo_order where order_no=#{orderNo}")
    DemoOrder getByOrderNo(String orderNo);

    @Insert("""
            insert into demo_order(order_no, user_id, product_name, sku_name, order_amount, pay_status, order_status, logistics_status, after_sale_status, paid_at, shipped_at, signed_at)
            values(#{orderNo}, #{userId}, #{productName}, #{skuName}, #{orderAmount}, #{payStatus}, #{orderStatus}, #{logisticsStatus}, coalesce(#{afterSaleStatus}, 'NONE'), #{paidAt}, #{shippedAt}, #{signedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(DemoOrder order);

    @Update("""
            update demo_order
            set order_no=#{orderNo}, user_id=#{userId}, product_name=#{productName}, sku_name=#{skuName},
                order_amount=#{orderAmount}, pay_status=#{payStatus}, order_status=#{orderStatus},
                logistics_status=#{logisticsStatus}, after_sale_status=#{afterSaleStatus},
                paid_at=#{paidAt}, shipped_at=#{shippedAt}, signed_at=#{signedAt}
            where id=#{id}
            """)
    void update(DemoOrder order);

    @Update("update demo_order set after_sale_status=#{status} where id=#{id}")
    void updateAfterSaleStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("delete from demo_order where id=#{id}")
    void delete(Long id);
}
