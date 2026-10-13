package com.schoolMarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 商品分类实体（一级分类），对应 category 表。
 */
@Data
public class Category {

    private Long id;

    /** 分类名，全表唯一 */
    private String name;

    private String description;

    private Date createTime;
}