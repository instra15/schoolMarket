package com.schoolMarket.mapper;

import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.entity.Product;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProductMapper {

    @Insert("insert into product(title,price,stock,status,category,version)" +
            "values(#{title},#{price},#{stock},#{status},#{category},#{version})")
    void insert(ProductCreateDTO productCreateDTO);

    @Select("select * from product where id=#{id}")
    Product findById(@Param("id") Long id);

    @Update("update product set" +
            "title=#{pro.title},price=#{pro.price},stock=#{pro.stock},status={pro.status},category=#{pro.category},version=#{pro.version}" +
            "where id=#{id}")
    void updateProduct(@Param("id") Long id,@Param("pro") ProductCreateDTO productCreateDTO);
}
