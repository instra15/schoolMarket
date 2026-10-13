package com.schoolMarket.service;

import com.schoolMarket.dto.CategoryCreateDTO;
import com.schoolMarket.vo.CategoryVO;

import java.util.List;

/**
 * 商品分类服务。
 */
public interface CategoryService {

    /**
     * 全部分类，附带各分类下的商品数量。
     */
    List<CategoryVO> listAll();

    /**
     * 按ID查分类。
     */
    CategoryVO getById(Long id);

    /**
     * 新建分类。分类名重复时抛 BusinessException。
     *
     * @return 新分类ID
     */
    Long create(CategoryCreateDTO dto);

    /**
     * 编辑分类。
     */
    void update(Long id, CategoryCreateDTO dto);

    /**
     * 删除分类。分类下仍有商品时拒绝删除。
     */
    void delete(Long id);
}