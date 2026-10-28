package com.schoolMarket.service.impl;

import com.schoolMarket.dto.CategoryCreateDTO;
import com.schoolMarket.entity.Category;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.CategoryMapper;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.service.CategoryService;
import com.schoolMarket.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /** 仅用于「分类下是否还有商品」的校验 */
    private final ProductMapper productMapper;

    @Override
    public List<CategoryVO> listAll() {
        return categoryMapper.findAllWithCount();
    }

    @Override
    public CategoryVO getById(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException("分类不存在：" + id);
        }

        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setDescription(category.getDescription());
        vo.setCreateTime(category.getCreateTime());
        vo.setProductCount(productMapper.countByCategory(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CategoryCreateDTO dto) {
        // name 上有唯一索引，这里先查一次给出友好提示。
        // 数据库的唯一索引仍是最后一道防线 —— 并发下仍可能抛 DuplicateKeyException，
        // 那时由 GlobalExceptionHandler 兜成 500。
        if (categoryMapper.findByName(dto.getName(), null) != null) {
            throw new BusinessException("分类名已存在：" + dto.getName());
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setCreateTime(new Date());

        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategoryCreateDTO dto) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException("分类不存在：" + id);
        }

        // 编辑时排除自身：没改名字就不该报「名字已存在」
        if (categoryMapper.findByName(dto.getName(), id) != null) {
            throw new BusinessException("分类名已存在：" + dto.getName());
        }

        categoryMapper.update(id, dto.getName(), dto.getDescription());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (categoryMapper.findById(id) == null) {
            throw new BusinessException("分类不存在：" + id);
        }

        // 分类下仍有商品时拒绝删除：product 表上有指向 category 的外键，
        // 直接 DELETE 会触发 MySQL 外键错误 1451，给用户看不懂的报错
        int productCount = productMapper.countByCategory(id);
        if (productCount > 0) {
            throw new BusinessException("该分类下还有 " + productCount + " 件商品，无法删除");
        }

        categoryMapper.deleteById(id);
    }
}