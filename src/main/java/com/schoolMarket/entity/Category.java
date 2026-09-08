package com.schoolMarket.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//商品分类（一级）

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Category {

    private Long id;

    private String name;

    private String description;

}
