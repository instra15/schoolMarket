package com.schoolMarket.entity;

import com.schoolMarket.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单主表实体，对应 order 表。一个订单含多个 OrderItem。
 *
 * <p>orderNo 由雪花算法生成，全局唯一且有序。表名 order 是 MySQL 关键字，
 * 所有 SQL 中必须写成 {@code `order`}。</p>
 */
@Data
public class Order {

    private Long id;

    /** 订单号，雪花算法生成，唯一索引 */
    private String orderNo;

    private Long buyerId;

    private BigDecimal totalPrice;

    private OrderStatus status;

    private Date createTime;

    private Date payTime;
}