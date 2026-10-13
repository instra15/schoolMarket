package com.schoolMarket.mapper;

import com.schoolMarket.dto.ProductQueryDTO;
import com.schoolMarket.entity.Product;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 Mapper。
 *
 * <p>SQL 全部写在 {@code resources/mapper/ProductMapper.xml}。
 * 本接口只声明方法，动态 SQL 与多表关联放在 XML 里可读性远好于字符串拼接。</p>
 */
@Mapper
public interface ProductMapper {

    /**
     * 新增商品。version 初始为 0、status 初始为 ON_SALE，均由服务端赋值，
     * 不接受前端传入（乐观锁版本号必须由服务端掌管）。
     */
    int insert(Product product);

    /**
     * 按ID查商品实体（含 version，Phase 3 扣库存用）。
     */
    Product findById(@Param("id") Long id);

    /**
     * 按ID查商品并映射为带分类/卖家信息的 VO（详情页）。
     */
    ProductVO findVOById(@Param("id") Long id);

    /**
     * 动态条件分页查询商品。
     *
     * @param query 查询条件，offset / pageSize 来自 {@code PageQueryDTO}
     */
    List<ProductVO> findVOByPage(@Param("q") ProductQueryDTO query);

    /**
     * 与 {@link #findVOByPage} 完全同条件的总数，用于算总页数。
     *
     * <p>必须与分页查询共用同一段 {@code <sql>} 片段，
     * 否则两边条件不一致会导致总数与实际结果对不上。</p>
     */
    long countByCondition(@Param("q") ProductQueryDTO query);

    /**
     * 编辑商品信息。只允许修改这几个字段，version 由 SQL 自增。
     *
     * @return 影响行数
     */
    int updateProduct(@Param("id") Long id,
                      @Param("title") String title,
                      @Param("description") String description,
                      @Param("price") BigDecimal price,
                      @Param("stock") Integer stock,
                      @Param("categoryId") Long categoryId);

    /**
     * 上下架切换。同时把 version 自增，避免与 Phase 3 的并发扣库存互相覆盖。
     *
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") ProductStatus status);

    /**
     * 删除商品。
     *
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}