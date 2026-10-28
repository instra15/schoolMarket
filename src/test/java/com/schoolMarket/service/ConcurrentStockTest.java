package com.schoolMarket.service;

import com.schoolMarket.dto.OrderCreateDTO;
import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Product;
import com.schoolMarket.entity.User;
import com.schoolMarket.enums.OrderStatus;
import com.schoolMarket.exception.StockNotEnoughException;
import com.schoolMarket.mapper.OrderItemMapper;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>并发扣库存防超卖测试 —— 本项目最核心的测试。</b>
 *
 * <p>直接验证乐观锁（version）是否真的防住了超卖。</p>
 *
 * <p><b>测试原理</b>：造一个库存为 1 的商品，让 N 个线程同时下单抢它。
 * 若乐观锁失效，N 个线程会全部读到 stock=1、各自算出 stock-1=0 并写回，
 * 数据库里会出现 N 条订单而库存只扣了 1 次 —— 即「丢失更新」式超卖。</p>
 *
 * <p><b>通过标准</b>：</p>
 * <ul>
 *   <li>成功下单数 ≤ 1</li>
 *   <li>数据库最终 stock ≥ 0（绝不为负）</li>
 *   <li>该商品的订单项条数 ≤ 1</li>
 * </ul>
 *
 * <p><b>为什么要用 CountDownLatch</b>：让所有线程在同一瞬间起跑。
 * 若各线程随机延迟发起，冲突概率降低，测试可能「假通过」——
 * 乐观锁有 bug 时也未必每次都复现。</p>
 */
@RequiresDatabase
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("并发扣库存防超卖测试")
class ConcurrentStockTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TestDataFactory factory;

    /**
     * 核心用例：20 个线程抢 1 件库存，断言最多成功 1 单。
     */
    @Test
    @DisplayName("20 线程抢 1 件库存：最多成功 1 单，库存不为负")
    void twentyThreadsRaceForOneStock() throws Exception {
        int threadCount = 20;

        User seller = factory.createUser("cst_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("并发分类_" + System.nanoTime());
        Product product = factory.createProduct("抢购商品", new BigDecimal("1.00"),
                1, category.getId(), seller.getId());

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger stockNotEnough = new AtomicInteger();
        AtomicInteger otherError = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            // 每个线程一个独立买家（不能买自己的商品）
            User buyer = factory.createUser("cst_buyer_" + i + "_" + System.nanoTime(), "买家" + i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    OrderCreateDTO dto = new OrderCreateDTO();
                    OrderCreateDTO.Item item = new OrderCreateDTO.Item();
                    item.setProductId(product.getId());
                    item.setQuantity(1);
                    dto.setItems(List.of(item));

                    orderService.createOrder(buyer.getId(), dto);
                    success.incrementAndGet();
                } catch (StockNotEnoughException e) {
                    // 预期结果：库存不足/乐观锁冲突
                    stockNotEnough.incrementAndGet();
                } catch (Exception e) {
                    otherError.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(30, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "并发测试超时");
        pool.shutdownNow();

        // ---- 核心断言 ----
        Product after = productMapper.findById(product.getId());
        long orderItemCount = countOrderItems(product.getId());

        System.out.println("并发结果: 成功=" + success.get()
                + " 库存不足=" + stockNotEnough.get()
                + " 其他错误=" + otherError.get()
                + " 最终库存=" + after.getStock()
                + " version=" + after.getVersion()
                + " 订单项=" + orderItemCount);

        assertTrue(success.get() <= 1,
                "超卖！成功下单 " + success.get() + " 单，但库存只有 1 件");
        assertTrue(after.getStock() >= 0,
                "超卖！最终库存为 " + after.getStock() + "，出现了负库存");
        assertEquals(0, after.getStock(),
                "库存为 1 且被抢走时，最终库存应为 0");
        assertTrue(orderItemCount <= 1,
                "超卖！产生了 " + orderItemCount + " 条订单项，库存只有 1 件");
        assertEquals(0, otherError.get(),
                "不应出现非预期的异常（其他错误 " + otherError.get() + " 次）");
    }

    /**
     * 库存充足时，所有并发都应成功，且库存扣减准确。
     */
    @Test
    @DisplayName("20 线程抢 10 件库存：库存充足时应全部成功，库存准确扣减")
    void twentyThreadsRaceForTenStock() throws Exception {
        int threadCount = 20;
        int stock = 10;

        User seller = factory.createUser("cst2_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("并发分类2_" + System.nanoTime());
        Product product = factory.createProduct("充足商品", new BigDecimal("1.00"),
                stock, category.getId(), seller.getId());

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            User buyer = factory.createUser("cst2_buyer_" + i + "_" + System.nanoTime(), "买家" + i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    OrderCreateDTO dto = new OrderCreateDTO();
                    OrderCreateDTO.Item item = new OrderCreateDTO.Item();
                    item.setProductId(product.getId());
                    item.setQuantity(1);
                    dto.setItems(List.of(item));
                    orderService.createOrder(buyer.getId(), dto);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(30, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(120, TimeUnit.SECONDS), "并发测试超时");
        pool.shutdownNow();

        Product after = productMapper.findById(product.getId());
        System.out.println("并发结果: 成功=" + success.get() + " 失败=" + failed.get()
                + " 最终库存=" + after.getStock());

        assertEquals(stock, success.get(),
                "库存 10 件、20 线程各买 1 件，应恰好成功 10 单");
        assertEquals(0, after.getStock(), "库存应被准确扣减为 0");
        assertEquals(0, failed.get(), "不应有失败");
    }

    /**
     * 乐观锁的直接验证：连续用同一个 version 扣两次，第二次必须失败。
     */
    @Test
    @DisplayName("乐观锁：同一 version 第二次扣减必失败")
    void optimisticLockRejectsStaleVersion() {
        User seller = factory.createUser("cst3_seller_" + System.nanoTime(), "卖家");
        Category category = factory.createCategory("并发分类3_" + System.nanoTime());
        Product product = factory.createProduct("版本商品", new BigDecimal("1.00"),
                5, category.getId(), seller.getId());

        int originalVersion = product.getVersion();

        // 第一次用正确 version 扣减
        int first = productMapper.deductStock(product.getId(), 1, originalVersion);
        assertEquals(1, first, "首次扣减应成功");

        // 第二次仍用旧 version —— 必须失败
        int second = productMapper.deductStock(product.getId(), 1, originalVersion);
        assertEquals(0, second, "过期的 version 必须被拒绝，这就是乐观锁");

        Product after = productMapper.findById(product.getId());
        assertEquals(4, after.getStock(), "只应扣减 1 件");
        assertEquals(originalVersion + 1, after.getVersion(), "version 只应自增 1 次");
    }

    /**
     * 同一事务内先扣成功再扣失败，必须整体回滚，不能留下「扣了一半」的脏数据。
     */
    @Test
    @DisplayName("事务回滚：一个商品扣成功、另一个失败时整体回滚")
    void transactionRollsBackOnPartialFailure() {
        User seller = factory.createUser("cst4_seller_" + System.nanoTime(), "卖家");
        User buyer = factory.createUser("cst4_buyer_" + System.nanoTime(), "买家");
        Category category = factory.createCategory("并发分类4_" + System.nanoTime());

        Product good = factory.createProduct("充足商品", new BigDecimal("10.00"),
                5, category.getId(), seller.getId());
        Product scarce = factory.createProduct("稀缺商品", new BigDecimal("20.00"),
                1, category.getId(), seller.getId());

        int goodStockBefore = good.getStock();

        OrderCreateDTO dto = new OrderCreateDTO();
        OrderCreateDTO.Item item1 = new OrderCreateDTO.Item();
        item1.setProductId(good.getId());
        item1.setQuantity(2);
        OrderCreateDTO.Item item2 = new OrderCreateDTO.Item();
        item2.setProductId(scarce.getId());
        item2.setQuantity(99);   // 远超库存，必然失败
        dto.setItems(List.of(item1, item2));

        assertThrows(StockNotEnoughException.class,
                () -> orderService.createOrder(buyer.getId(), dto));

        // 关键断言：先扣的那个商品的库存也必须回滚
        Product goodAfter = productMapper.findById(good.getId());
        assertEquals(goodStockBefore, goodAfter.getStock(),
                "事务应整体回滚：另一个商品扣减失败时，已扣的库存必须回滚");
        assertEquals(0, goodAfter.getVersion(),
                "回滚后 version 也应回到原值");
    }

    /**
     * 统计某商品产生的订单项条数。
     *
     * <p>直接查 order_item 表最准确。
     * 不用 {@code findPageBySeller} —— 那个方法的参数是 sellerId，
     * 把 productId 传进去是语义错误（虽然恰好也能查到部分数据，但不可靠）。</p>
     */
    private long countOrderItems(Long productId) {
        return orderItemMapper.countByProduct(productId);
    }
}
