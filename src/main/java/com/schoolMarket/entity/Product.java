package com.schoolMarket.entity;

//商品表

import com.schoolMarket.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    private Long id;

    private String title;

    private BigDecimal price;

    private Integer stock;

    private ProductStatus status;

    private Category category;

    private Integer version;

}
