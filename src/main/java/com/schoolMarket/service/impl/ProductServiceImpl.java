package com.schoolMarket.service.impl;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.dto.ProductQueryDTO;
import com.schoolMarket.dto.ProductUpdateDTO;
import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Product;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.CategoryMapper;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.service.ProductService;
import com.schoolMarket.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long sellerId, ProductCreateDTO dto) {
        requireCategoryExists(dto.getCategoryId());

        Product product = new Product();
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setImageUrl(dto.getImageUrl());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        // 上架初始状态由服务端决定
        product.setStatus(ProductStatus.ON_SALE);
        product.setCategoryId(dto.getCategoryId());
        // 卖家来自登录态，不接受前端传入
        product.setSellerId(sellerId);
        // 乐观锁初始版本号
        product.setVersion(0);

        Date now = new Date();
        product.setCreateTime(now);
        product.setUpdateTime(now);

        productMapper.insert(product);
        return product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long sellerId, Long productId, ProductUpdateDTO dto) {
        Product product = requireOwned(sellerId, productId);
        requireCategoryExists(dto.getCategoryId());

        // 已售罄的商品不允许再改库存，只能先上架再调
        if (product.getStock() == 0 && dto.getStock() > 0 && product.getStatus() == ProductStatus.OFF_SALE) {
            throw new BusinessException("商品已下架，请先上架后再调整库存");
        }

        productMapper.updateProduct(productId, dto.getTitle(), dto.getDescription(),
                dto.getPrice(), dto.getStock(), dto.getCategoryId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long sellerId, Long productId, ProductStatus status) {
        Product product = requireOwned(sellerId, productId);

        if (status == ProductStatus.ON_SALE && product.getStock() <= 0) {
            throw new BusinessException("库存为 0，无法上架");
        }
        if (product.getStatus() == status) {
            throw new BusinessException("商品已经是「" + status + "」状态");
        }

        productMapper.updateStatus(productId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long sellerId, Long productId) {
        Product product = requireOwned(sellerId, productId);

        if (product.getStock() > 0) {
            throw new BusinessException("商品仍有库存 " + product.getStock() + " 件，请先下架并清空库存后再删除");
        }

        productMapper.deleteById(productId);
    }

    @Override
    public PageResult<ProductVO> search(ProductQueryDTO query) {
        return doPage(query);
    }

    @Override
    public PageResult<ProductVO> searchMine(Long sellerId, ProductQueryDTO query) {
        // 强制覆盖为当前登录用户，防止前端传别人的 sellerId
        query.setSellerId(sellerId);
        return doPage(query);
    }

    @Override
    public ProductVO getDetail(Long productId) {
        ProductVO vo = productMapper.findVOById(productId);
        if (vo == null) {
            throw new BusinessException("商品不存在或已下架：" + productId);
        }
        return vo;
    }

    /**
     * 分页查询 + 总数统计。两条查询共用 XML 里的 pageWhere 片段，条件必然一致。
     */
    private PageResult<ProductVO> doPage(ProductQueryDTO query) {
        // 价格区间校验：下限不能大于上限
        if (query.getMinPrice() != null && query.getMaxPrice() != null
                && query.getMinPrice().compareTo(query.getMaxPrice()) > 0) {
            throw new BusinessException("价格下限不能大于价格上限");
        }

        long total = productMapper.countByCondition(query);
        List<ProductVO> list = total == 0 ? List.of() : productMapper.findVOByPage(query);

        return PageResult.of(total, query.getPageNum(), query.getPageSize(), list);
    }

    /**
     * 校验商品存在且属于该卖家 —— 越权访问的核心防线。
     */
    private Product requireOwned(Long sellerId, Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new BusinessException("商品不存在：" + productId);
        }
        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException("只能操作自己的商品");
        }
        return product;
    }

    private void requireCategoryExists(Long categoryId) {
        Category category = categoryMapper.findById(categoryId);
        if (category == null) {
            throw new BusinessException("分类不存在：" + categoryId);
        }
    }
}