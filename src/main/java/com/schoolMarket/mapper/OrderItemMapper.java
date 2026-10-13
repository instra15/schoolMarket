package com.schoolMarket.mapper;

import com.schoolMarket.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单项 Mapper。
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入订单项，用 {@code <foreach>} 拼 VALUES。
     *
     * <p>相比循环单条 insert，批量插入只需一次网络往返，
     * 且 JDBC 开了 {@code rewriteBatchedStatements=true} 时性能差距更明显。</p>
     */
    int batchInsert(@Param("items") List<OrderItem> items);

    /**
     * 查某订单的全部订单项。
     *
     * <p>用于取消订单时回补库存 —— 只需 order_item 表本身，
     * 不必JOIN product，避免无谓的关联查询。</p>
     */
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
}