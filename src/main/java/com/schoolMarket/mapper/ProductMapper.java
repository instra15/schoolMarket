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

    /**
     * 统计某分类下有多少商品。
     *
     * <p>放在 ProductMapper 而非 CategoryMapper：它数的是 product 表，
     * 而非 category 表。删除分类前用它做校验，避免触发 MySQL 外键错误 1451。</p>
     */
    int countByCategory(@Param("categoryId") Long categoryId);

    /**
     * <b>乐观锁扣库存 —— 本项目的并发核心。</b>
     *
     * <p>SQL 带三个条件：</p>
     * <ul>
     *   <li>{@code id = ?} —— 定位商品</li>
     *   <li>{@code version = ?} —— 乐观锁：version 必须与读取时一致，
     *       说明期间没被别人改过。这是防超卖的关键：
     *       没有它，两个线程都会读到 stock=1，各自算出扣减后都是 0，
     *       后写入的直接覆盖前者 →库存被扣成 -1（丢失更新）</li>
     *   <li>{@code stock >= ?} —— 兜底：即使version 判断被绕过，
     *       也绝不允许扣成负数</li>
     * </ul>
     *
     * @return 影响行数。<b>返回 0 表示失败</b>：
     *         要么 version 被别人改过（并发冲突），要么库存不足。
     *         调用方必须检查这个返回值，为 0 时抛 StockNotEnoughException 触发回滚。
     */
    int deductStock(@Param("productId") Long productId,
                    @Param("quantity") Integer quantity,
                    @Param("version") Integer version);

    /**
     * 回补库存（取消订单时用）。同样递增 version 保持并发语义一致。
     *
     * @return 影响行数
     */
    int restoreStock(@Param("productId") Long productId,
                     @Param("quantity") Integer quantity);
}