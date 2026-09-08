package com.schoolMarket.entity;

//订单主表

import com.schoolMarket.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    private Long id;

    private String orderNo;

    private User buyer;

    private BigDecimal totalPrice;

    private OrderStatus status;

}
