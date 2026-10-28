package com.schoolMarket.mapper;

import com.schoolMarket.dto.ProductQueryDTO;
import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Product;
import com.schoolMarket.entity.User;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import com.schoolMarket.vo.ProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductMapper 单元测试。
 *
 * <p>重点验证三件事：</p>
 * <ol>
 *   <li><b>动态 SQL</b>：各条件组合下的 WHERE 拼接是否正确</li>
 *   <li><b>手写分页</b>：LIMIT/OFFSET 与 count 是否一致</li>
 *   <li><b>association 关联</b>：分类名与卖家昵称是否被正确带出</li>
 * </ol>
 */
@RequiresDatabase
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestDataFactory.class)
@ActiveProfiles("test")
@DisplayName("ProductMapper 单元测试")
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private TestDataFactory factory;

    private Long categoryId;
    private Long otherCategoryId;
    private Long sellerId;
    private Long otherSellerId;

    private void setUp() {
        User seller = factory.createUser("pm_seller_" + System.nanoTime(), "卖家甲");
        User other = factory.createUser("pm_other_" + System.nanoTime(), "卖家乙");
        sellerId = seller.getId();
        otherSellerId = other.getId();

        Category c1 = factory.createCategory("分类甲_" + System.nanoTime());
        Category c2 = factory.createCategory("分类乙_" + System.nanoTime());
        categoryId = c1.getId();
        otherCategoryId = c2.getId();
    }

    @Test
    @DisplayName("插入商品后 version 初始为 0 且可按ID查回")
    void insertAndFindById() {
        setUp();
        Product p = factory.createProduct("测试商品", new BigDecimal("25.00"), 5, categoryId, sellerId);

        assertNotNull(p.getId(), "useGeneratedKeys 应回填自增ID");
        assertEquals(0, p.getVersion());

        Product found = productMapper.findById(p.getId());
        assertNotNull(found);
        assertEquals("测试商品", found.getTitle());
        assertEquals(5, found.getStock());
        assertEquals(ProductStatus.ON_SALE, found.getStatus());
        assertEquals(categoryId, found.getCategoryId());
        assertEquals(sellerId, found.getSellerId());
    }

    @Test
    @DisplayName("findVOById 通过 association 带出分类名与卖家昵称")
    void findVOByIdWithAssociation() {
        setUp();
        Product p = factory.createProduct("关联测试商品", new BigDecimal("88.50"), 3, categoryId, sellerId);

        ProductVO vo = productMapper.findVOById(p.getId());
        assertNotNull(vo);
        // association 映射的核心断言
        assertNotNull(vo.getCategory(), "category 关联对象不应为 null");
        assertNotNull(vo.getSeller(), "seller 关联对象不应为 null");
        assertEquals(categoryId, vo.getCategory().getId());
        assertEquals(sellerId, vo.getSeller().getId());
        assertEquals("卖家甲", vo.getSeller().getNickname());
        assertEquals(p.getTitle(), vo.getTitle());
        assertEquals(new BigDecimal("88.50"), vo.getPrice());
    }

    @Test
    @DisplayName("动态SQL：按分类过滤")
    void searchByCategory() {
        setUp();
        factory.createProduct("甲类商品1", new BigDecimal("10.00"), 1, categoryId, sellerId);
        factory.createProduct("甲类商品2", new BigDecimal("20.00"), 1, categoryId, sellerId);
        factory.createProduct("乙类商品", new BigDecimal("30.00"), 1, otherCategoryId, otherSellerId);

        ProductQueryDTO query = new ProductQueryDTO();
        query.setCategoryId(categoryId);

        List<ProductVO> list = productMapper.findVOByPage(query);
        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(v -> categoryId.equals(v.getCategory().getId())));
        assertEquals(2, productMapper.countByCondition(query));
    }

    @Test
    @DisplayName("动态SQL：关键词对标题与描述做模糊匹配")
    void searchByKeyword() {
        setUp();
        factory.createProduct("数据结构教材", new BigDecimal("25.00"), 1, categoryId, sellerId);
        factory.createProduct("考研数学资料", new BigDecimal("88.00"), 1, categoryId, sellerId);
        factory.createProduct("小米降噪耳机", new BigDecimal("199.00"), 1, otherCategoryId, otherSellerId);

        ProductQueryDTO query = new ProductQueryDTO();
        query.setKeyword("教材");

        List<ProductVO> list = productMapper.findVOByPage(query);
        assertEquals(1, list.size());
        assertEquals("数据结构教材", list.get(0).getTitle());
        assertEquals(1, productMapper.countByCondition(query));
    }

    @Test
    @DisplayName("动态SQL：价格区间过滤（含边界）")
    void searchByPriceRange() {
        setUp();
        factory.createProduct("便宜货", new BigDecimal("10.00"), 1, categoryId, sellerId);
        factory.createProduct("中等货", new BigDecimal("50.00"), 1, categoryId, sellerId);
        factory.createProduct("贵货", new BigDecimal("200.00"), 1, categoryId, sellerId);

        ProductQueryDTO query = new ProductQueryDTO();
        query.setMinPrice(new BigDecimal("10.00"));
        query.setMaxPrice(new BigDecimal("50.00"));

        List<ProductVO> list = productMapper.findVOByPage(query);
        assertEquals(2, list.size(), "价格区间应含边界值");
        assertEquals(2, productMapper.countByCondition(query));
    }

    @Test
    @DisplayName("动态SQL：无条件时返回全部")
    void searchWithoutCondition() {
        setUp();
        factory.createProduct("商品A", new BigDecimal("10.00"), 1, categoryId, sellerId);
        factory.createProduct("商品B", new BigDecimal("20.00"), 1, otherCategoryId, otherSellerId);

        ProductQueryDTO query = new ProductQueryDTO();
        List<ProductVO> list = productMapper.findVOByPage(query);
        assertTrue(list.size() >= 2, "无条件时应返回全部商品");
        assertEquals(productMapper.countByCondition(query), list.size(),
                "count 与列表在无条件时必须一致");
    }

    @Test
    @DisplayName("手写分页：LIMIT/OFFSET 正确且与 count 一致")
    void pagination() {
        setUp();
        for (int i = 1; i <= 5; i++) {
            factory.createProduct("分页商品" + i, new BigDecimal("10.00"), 1, categoryId, sellerId);
        }

        ProductQueryDTO query = new ProductQueryDTO();
        query.setCategoryId(categoryId);
        query.setPageNum(1);
        query.setPageSize(2);

        List<ProductVO> firstPage = productMapper.findVOByPage(query);
        assertEquals(2, firstPage.size(), "第1页应有2条");
        assertEquals(5, productMapper.countByCondition(query), "总数应为5");
        assertEquals(0, query.getOffset(), "第1页 offset 应为0");

        // 第2页
        query.setPageNum(2);
        List<ProductVO> secondPage = productMapper.findVOByPage(query);
        assertEquals(2, secondPage.size(), "第2页应有2条");
        assertEquals(2, query.getOffset(), "第2页 offset 应为2");

        // 第3页只剩1条
        query.setPageNum(3);
        List<ProductVO> thirdPage = productMapper.findVOByPage(query);
        assertEquals(1, thirdPage.size(), "第3页应剩1条");
        assertEquals(4, query.getOffset());

        // 三页数据不应重复
        List<Long> ids = new java.util.ArrayList<>();
        firstPage.forEach(v -> ids.add(v.getId()));
        secondPage.forEach(v -> ids.add(v.getId()));
        thirdPage.forEach(v -> ids.add(v.getId()));
        assertEquals(5, ids.stream().distinct().count(), "分页结果不应有重复");
    }

    @Test
    @DisplayName("扣库存：version 与 stock 双条件同时校验")
    void deductStock() {
        setUp();
        Product p = factory.createProduct("扣库存商品", new BigDecimal("10.00"), 5, categoryId, sellerId);

        // 正常扣减
        int rows = productMapper.deductStock(p.getId(), 2, p.getVersion());
        assertEquals(1, rows, "version 匹配时扣减应成功");

        Product after = productMapper.findById(p.getId());
        assertEquals(3, after.getStock(), "库存应减少2");
        assertEquals(1, after.getVersion(), "version 应自增");

        // version 不匹配 → 影响 0 行
        int stale = productMapper.deductStock(p.getId(), 1, p.getVersion());
        assertEquals(0, stale, "version 过期时应影响 0 行");

        // 库存不足 → 影响 0 行
        int notEnough = productMapper.deductStock(p.getId(), 100, after.getVersion());
        assertEquals(0, notEnough, "库存不足时应影响 0 行");
    }

    @Test
    @DisplayName("回补库存：库存增加且 version 自增")
    void restoreStock() {
        setUp();
        Product p = factory.createProduct("回补商品", new BigDecimal("10.00"), 3, categoryId, sellerId);

        int rows = productMapper.restoreStock(p.getId(), 2);
        assertEquals(1, rows);

        Product after = productMapper.findById(p.getId());
        assertEquals(5, after.getStock());
        assertEquals(1, after.getVersion());
    }

    @Test
    @DisplayName("统计分类下商品数")
    void countByCategory() {
        setUp();
        factory.createProduct("商品1", new BigDecimal("10.00"), 1, categoryId, sellerId);
        factory.createProduct("商品2", new BigDecimal("10.00"), 1, categoryId, sellerId);
        factory.createProduct("商品3", new BigDecimal("10.00"), 1, otherCategoryId, otherSellerId);

        assertEquals(2, productMapper.countByCategory(categoryId));
        assertEquals(1, productMapper.countByCategory(otherCategoryId));
    }
}