package com.schoolMarket.mapper;

import com.schoolMarket.entity.Category;
import com.schoolMarket.entity.Order;
import com.schoolMarket.entity.OrderItem;
import com.schoolMarket.entity.Product;
import com.schoolMarket.entity.User;
import com.schoolMarket.enums.OrderStatus;
import com.schoolMarket.support.RequiresDatabase;
import com.schoolMarket.support.TestDataFactory;
import com.schoolMarket.vo.OrderVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderMapper 单元测试。
 *
 * <p>重点验证：</p>
 * <ul>
 *   <li><b>collection 一对多</b>：订单项列表能被正确带出且不重复</li>
 *   <li><b>支付/取消幂等</b>：SQL 的 status 条件是否真的起作用</li>
 *   <li>卖家视角查询（order 表不存 seller_id，靠 EXISTS 子查询）</li>
 * </ul>
 */
@RequiresDatabase
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestDataFactory.class)
@ActiveProfiles("test")
@DisplayName("OrderMapper 单元测试")
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private TestDataFactory factory;

    private Long sellerId;
    private Long buyerId;
    private Long categoryId;

    private void setUp() {
        User seller = factory.createUser("om_seller_" + System.nanoTime(), "卖家");
        User buyer = factory.createUser("om_buyer_" + System.nanoTime(), "买家");
        sellerId = seller.getId();
        buyerId = buyer.getId();
        Category category = factory.createCategory("订单分类_" + System.nanoTime());
        categoryId = category.getId();
    }

    /**
     * 造一个订单并挂上订单项。
     */
    private Order createOrder(String orderNo, User buyer, Product p1, int qty1,
                              Product p2, int qty2) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyer.getId());
        order.setTotalPrice(p1.getPrice().multiply(BigDecimal.valueOf(qty1))
                .add(p2.getPrice().multiply(BigDecimal.valueOf(qty2))));
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(new Date());
        orderMapper.insert(order);

        List<OrderItem> items = List.of(
                buildItem(order.getId(), p1.getId(), qty1, p1.getPrice()),
                buildItem(order.getId(), p2.getId(), qty2, p2.getPrice()));
        orderItemMapper.batchInsert(items);
        return order;
    }

    private OrderItem buildItem(Long orderId, Long productId, int qty, BigDecimal price) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setPrice(price);
        return item;
    }

    @Test
    @DisplayName("插入订单并用 <collection> 带出全部订单项")
    void findVOByOrderNoWithCollection() {
        setUp();
        Product p1 = factory.createProduct("商品A", new BigDecimal("25.00"), 5, categoryId, sellerId);
        Product p2 = factory.createProduct("商品B", new BigDecimal("10.00"), 3, categoryId, sellerId);

        Order order = createOrder("TEST" + System.nanoTime(), factory.createUser("om_b_" + System.nanoTime(), "买家"),
                p1, 2, p2, 1);

        OrderVO vo = orderMapper.findVOByOrderNo(order.getOrderNo());
        assertNotNull(vo);
        assertNotNull(vo.getItems(), "collection 应带出订单项列表");
        assertEquals(2, vo.getItems().size(), "应带出 2 个订单项");

        // 订单项内容正确
        assertTrue(vo.getItems().stream()
                        .anyMatch(i -> p1.getId().equals(i.getProductId()) && i.getQuantity() == 2),
                "应包含商品A且数量为2");
        assertTrue(vo.getItems().stream()
                        .anyMatch(i -> p2.getId().equals(i.getProductId()) && i.getQuantity() == 1),
                "应包含商品B且数量为1");

        // JOIN 带出的商品标题
        assertTrue(vo.getItems().stream()
                        .anyMatch(i -> "商品A".equals(i.getProductTitle())),
                "collection 内应带出商品标题");
    }

    @Test
    @DisplayName("支付：SQL 带 status 条件实现幂等")
    void payIsIdempotent() {
        setUp();
        Product p = factory.createProduct("支付商品", new BigDecimal("10.00"), 5, categoryId, sellerId);
        Order order = createOrder("PAY" + System.nanoTime(),
                factory.createUser("om_p_" + System.nanoTime(), "买家"), p, 1, p, 1);

        // 首次支付
        int first = orderMapper.pay(order.getOrderNo(), new Date());
        assertEquals(1, first, "首次支付应影响 1 行");

        Order paid = orderMapper.findByOrderNo(order.getOrderNo());
        assertEquals(OrderStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPayTime(), "支付时间应被写入");

        // 重复支付：影响 0 行，且 pay_time 不变
        Date originalPayTime = paid.getPayTime();
        int second = orderMapper.pay(order.getOrderNo(), new Date());
        assertEquals(0, second, "重复支付应影响 0 行（幂等）");

        Order after = orderMapper.findByOrderNo(order.getOrderNo());
        assertEquals(originalPayTime, after.getPayTime(), "重复支付不应覆盖支付时间");
    }

    @Test
    @DisplayName("取消订单：幂等，且已支付订单不能取消")
    void cancelIsIdempotent() {
        setUp();
        Product p = factory.createProduct("取消商品", new BigDecimal("10.00"), 5, categoryId, sellerId);
        Order order = createOrder("CANCEL" + System.nanoTime(),
                factory.createUser("om_c_" + System.nanoTime(), "买家"), p, 1, p, 1);

        int first = orderMapper.cancel(order.getOrderNo());
        assertEquals(1, first);

        int second = orderMapper.cancel(order.getOrderNo());
        assertEquals(0, second, "重复取消应影响 0 行（幂等）");

        // 已支付的订单取消不了
        Order paidOrder = createOrder("PAIDCANCEL" + System.nanoTime(),
                factory.createUser("om_pc_" + System.nanoTime(), "买家"), p, 1, p, 1);
        orderMapper.pay(paidOrder.getOrderNo(), new Date());
        assertEquals(0, orderMapper.cancel(paidOrder.getOrderNo()),
                "已支付订单取消应影响 0 行");
    }

    @Test
    @DisplayName("批量插入订单项")
    void batchInsertItems() {
        setUp();
        Product p1 = factory.createProduct("批量A", new BigDecimal("1.00"), 1, categoryId, sellerId);
        Product p2 = factory.createProduct("批量B", new BigDecimal("2.00"), 1, categoryId, sellerId);

        Order order = new Order();
        order.setOrderNo("BATCH" + System.nanoTime());
        order.setBuyerId(buyerId);
        order.setTotalPrice(new BigDecimal("5.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(new Date());
        orderMapper.insert(order);

        int rows = orderItemMapper.batchInsert(List.of(
                buildItem(order.getId(), p1.getId(), 1, p1.getPrice()),
                buildItem(order.getId(), p2.getId(), 2, p2.getPrice())));
        assertEquals(2, rows, "应插入 2 条订单项");

        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        assertEquals(2, items.size());
    }

    @Test
    @DisplayName("卖家视角：靠 EXISTS 子查询按 seller_id 过滤")
    void findPageBySeller() {
        setUp();
        User buyer = factory.createUser("om_buyer2_" + System.nanoTime(), "买家");
        User otherSeller = factory.createUser("om_seller2_" + System.nanoTime(), "别的卖家");

        Product mine = factory.createProduct("我的商品", new BigDecimal("10.00"), 5, categoryId, sellerId);
        Product theirs = factory.createProduct("别人的商品", new BigDecimal("20.00"), 5, categoryId, otherSeller.getId());

        Order myOrder = createOrder("MINE" + System.nanoTime(), buyer, mine, 1, mine, 1);
        createOrder("THEIRS" + System.nanoTime(), buyer, theirs, 1, theirs, 1);

        var query = new com.schoolMarket.dto.OrderQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        List<OrderVO> list = orderMapper.findPageBySeller(sellerId, query);
        assertEquals(1, list.size(), "卖家应只看到自己商品的订单");
        assertEquals(myOrder.getOrderNo(), list.get(0).getOrderNo());

        long count = orderMapper.countBySeller(sellerId, query);
        assertEquals(1, count, "count 与列表必须一致");
    }

    @Test
    @DisplayName("买家视角：count 用 COUNT(DISTINCT) 不因 JOIN 放大")
    void countByBuyerNotInflated() {
        setUp();
        User buyer = factory.createUser("om_buyer3_" + System.nanoTime(), "买家");
        Product p = factory.createProduct("计数商品", new BigDecimal("10.00"), 5, categoryId, sellerId);

        // 一个订单含 3 个订单项 —— 若用 COUNT(*) 会算成 3
        Order order = createOrder("COUNT" + System.nanoTime(), buyer, p, 1, p, 1);
        orderItemMapper.batchInsert(List.of(buildItem(order.getId(), p.getId(), 1, p.getPrice())));

        var query = new com.schoolMarket.dto.OrderQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        long count = orderMapper.countByBuyer(buyer.getId(), query);
        assertEquals(1, count, "一个订单只能算 1 次，不能被 JOIN 放大");
    }
}