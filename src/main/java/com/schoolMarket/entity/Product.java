package com.schoolMarket.entity;

import com.schoolMarket.enums.ProductStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体，对应 product 表。
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li>外键用 {@code categoryId} / {@code sellerId} 扁平存储，不嵌套 Category/User 对象。
 *       Entity 必须与表字段一一对应，否则 {@code #{category}} 绑定 category_id 必然报错。</li>
 *   <li>{@code version} 是乐观锁字段，每次 UPDATE 自增。Phase 3 扣库存时
 *       SQL 需同时带 {@code version = #{version}} 和 {@code stock >= #{quantity}}。</li>
 *   <li>status 映射 varchar，MyBatis 内置 EnumTypeHandler 零配置处理。</li>
 * </ul>
 */
@Data
public class Product {

    private Long id;

    private String title;

    private String description;

    private String imageUrl;

    private BigDecimal price;

    /** 库存，扣减后不得为负 */
    private Integer stock;

    private ProductStatus status;

    private Long categoryId;

    private Long sellerId;

    /** 乐观锁版本号 */
    private Integer version;

    private Date createTime;

    private Date updateTime;
}