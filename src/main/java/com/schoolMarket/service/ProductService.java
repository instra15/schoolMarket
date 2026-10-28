package com.schoolMarket.service;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.ProductQueryDTO;
import com.schoolMarket.dto.ProductUpdateDTO;
import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.vo.ProductVO;

/**
 * 商品服务。
 *
 * <p>同UserService：不返回 Result 包装体，业务失败一律抛 {@code BusinessException}。</p>
 */
public interface ProductService {

    /**
     * 发布商品。sellerId 由登录用户决定，不接受前端传入。
     *
     * @return 新商品ID
     */
    Long publish(Long sellerId, ProductCreateDTO dto);

    /**
     * 编辑商品。校验归属，只能改自己的商品。
     */
    void update(Long sellerId, Long productId, ProductUpdateDTO dto);

    /**
     * 上下架切换。校验归属。
     *
     * @param status 目标状态
     */
    void changeStatus(Long sellerId, Long productId, ProductStatus status);

    /**
     * 删除商品。校验归属，且库存必须为 0 才允许删除
     *（否则直接 DELETE 会破坏外键约束或留下错误库存）。
     */
    void delete(Long sellerId, Long productId);

    /**
     * 分页搜索商品（动态条件 + 分类/卖家关联）。
     */
    PageResult<ProductVO> search(ProductQueryDTO query);

    /**
     * 我的商品列表（按卖家过滤，复用同一套分页逻辑）。
     */
    PageResult<ProductVO> searchMine(Long sellerId, ProductQueryDTO query);

    /**
     * 商品详情。
     */
    ProductVO getDetail(Long productId);
}