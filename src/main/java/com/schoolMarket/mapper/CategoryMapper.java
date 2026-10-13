package com.schoolMarket.mapper;

import com.schoolMarket.entity.Category;
import com.schoolMarket.vo.CategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品分类 Mapper。
 */
@Mapper
public interface CategoryMapper {

    /**
     * 查全部分类，附带每个分类下的商品数量。
     */
    List<CategoryVO> findAllWithCount();

    /**
     * 按ID查分类。
     */
    Category findById(@Param("id") Long id);

    /**
     * 按分类名查。用于「分类名是否重复」的校验，
     * 避免为此把整张表拉回来（findAllWithCount 是全表 + GROUP BY）。
     *
     * @param excludeId 要排除的分类ID，编辑场景下传自身ID；新增场景传 null
     * @return 命中返回该分类，未命中返回 null
     */
    Category findByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 新增分类。
     */
    int insert(Category category);

    /**
     * 更新分类名与描述。
     */
    int update(@Param("id") Long id,
               @Param("name") String name,
               @Param("description") String description);

    /**
     * 删除分类。
     *
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}