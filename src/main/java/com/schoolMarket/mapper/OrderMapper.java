package com.schoolMarket.mapper;

import com.schoolMarket.dto.OrderQueryDTO;
import com.schoolMarket.entity.Order;
import com.schoolMarket.enums.OrderStatus;
import com.schoolMarket.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 订单 Mapper。
 *
 * <p>注意表名 {@code order} 是 MySQL 关键字，XML 里必须写成 {@code `order`}。</p>
 */
@Mapper
public interface OrderMapper {

    /**
     * 新增订单主记录，用 useGeneratedKeys 回填自增 id 到 Order.id。
     */
    int insert(Order order);

    /**
     * 按订单号查订单实体。
     */
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 按订单号查订单并映射为带订单项列表的 VO。
     *
     * <p>用 {@code <collection>} 一次带出所有订单项，
     * MyBatis 会自动去重合并成「一个订单 + 多个订单项」的结构。</p>
     */
    OrderVO findVOByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 支付：把订单从 PENDING 改为 PAID。
     *
     * <p><b>SQL 里带 {@code status = 'PENDING'} 条件实现幂等</b>：
     * 重复调用时第二次影响行数为 0，Service 据此判断「已经是已支付」，
     * 不会重复更新 pay_time，也避免并发下状态被覆盖。</p>
     *
     * @return 影响行数
     */
    int pay(@Param("orderNo") String orderNo, @Param("payTime") Date payTime);

    /**
     * 取消订单：PENDING → CANCELLED。同样带状态条件保证幂等。
     *
     * @return 影响行数
     */
    int cancel(@Param("orderNo") String orderNo);

    /**
     * 买家视角：我的订单分页查询。
     */
    List<OrderVO> findPageByBuyer(@Param("buyerId") Long buyerId, @Param("q") OrderQueryDTO query);

    long countByBuyer(@Param("buyerId") Long buyerId, @Param("q") OrderQueryDTO query);

    /**
     * 卖家视角：我卖出的订单分页查询。
     *
     * <p>需要 JOIN order_item → product 才能拿到 seller_id，
     * 因为 order 表本身不存卖家（一个订单可能包含多个不同卖家的商品）。</p>
     */
    List<OrderVO> findPageBySeller(@Param("sellerId") Long sellerId, @Param("q") OrderQueryDTO query);

    long countBySeller(@Param("sellerId") Long sellerId, @Param("q") OrderQueryDTO query);
}