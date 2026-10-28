package com.schoolMarket.service;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.OrderCreateDTO;
import com.schoolMarket.dto.OrderQueryDTO;
import com.schoolMarket.vo.OrderVO;

/**
 * 订单服务 —— 本项目并发控制的核心所在。
 */
public interface OrderService {

    /**
     * 下单。
     *
     * <p>整个方法在同一个事务里，保证「扣库存 + 生成订单 + 生成订单项」全有或全无：
     * 任何一个商品扣库存失败，已扣的全部回滚，不会出现「扣了库存却没有订单」。</p>
     *
     * @param buyerId 买家ID，取自登录态
     * @return 订单号
     */
    String createOrder(Long buyerId, OrderCreateDTO dto);

    /**
     * 模拟支付。幂等：已支付时重复调用直接返回成功，不重复更新支付时间。
     */
    void pay(Long buyerId, String orderNo);

    /**
     * 取消订单，并把库存回补。幂等。
     */
    void cancel(Long buyerId, String orderNo);

    /**
     * 订单详情（含订单项列表）。非本人订单不可见。
     */
    OrderVO getDetail(Long userId, String orderNo);

    /**
     * 我买到的订单。
     */
    PageResult<OrderVO> pageAsBuyer(Long buyerId, OrderQueryDTO query);

    /**
     * 我卖出的订单。
     */
    PageResult<OrderVO> pageAsSeller(Long sellerId, OrderQueryDTO query);
}