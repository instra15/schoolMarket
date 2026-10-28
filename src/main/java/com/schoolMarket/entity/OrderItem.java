package com.schoolMarket.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项实体，对应 order_item 表。订单与商品是一对多。
 *
 * <p>{@code price} 存下单时刻的单价快照，即使商品后续调价也不影响历史订单。</p>
 */
@Data
public class OrderItem {

    private Long id;

    private Long orderId;

    private Long productId;

    /** 购买数量。注意是整数而非小数 —— 原代码误写为 BigDecimal */
    private Integer quantity;

    /** 下单时单价快照 */
    private BigDecimal price;
}