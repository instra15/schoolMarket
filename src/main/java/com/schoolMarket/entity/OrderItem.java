package com.schoolMarket.entity;

//订单项（一对多）

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {

    private Long id;

    private Order order;

    private Product product;

    private BigDecimal quantity;

    private BigDecimal price;

}
