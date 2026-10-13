package com.schoolMarket.service.impl;

import com.schoolMarket.common.PageResult;
import com.schoolMarket.dto.OrderCreateDTO;
import com.schoolMarket.dto.OrderQueryDTO;
import com.schoolMarket.entity.Order;
import com.schoolMarket.entity.OrderItem;
import com.schoolMarket.entity.Product;
import com.schoolMarket.enums.OrderStatus;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.exception.StockNotEnoughException;
import com.schoolMarket.mapper.OrderItemMapper;
import com.schoolMarket.mapper.OrderMapper;
import com.schoolMarket.mapper.ProductMapper;
import com.schoolMarket.service.OrderService;
import com.schoolMarket.util.SnowflakeIdGenerator;
import com.schoolMarket.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * ==================== 下单：事务编排核心 ====================
     *
     * <p>执行顺序（缺一不可）：</p>
     * <ol>
     *   <li>归一化：合并同一商品的重复行</li>
     *   <li>读取全部商品的 stock / version / price / status / sellerId</li>
     *   <li>校验：存在、未下架、不是自己的商品</li>
     *   <li><b>按 productId 升序逐个扣库存</b>（乐观锁）</li>
     *   <li>生成订单主记录</li>
     *   <li>批量插入订单项</li>
     * </ol>
     *
     * <p><b>为什么必须按 productId 升序扣减？</b>
     * 两个订单若分别包含 [商品1, 商品2] 与 [商品2, 商品1]，
     * 各自按传入顺序加锁会形成交叉等待，极易死锁。
     * 统一升序后加锁顺序全局一致，死锁概率大幅降低。</p>
     *
     * <p><b>事务边界</b>：此处只含核心交易数据。
     * 通知与操作日志的异步写入放在 Phase 4 —— 刻意不放进本事务，
     * 避免辅助数据的失败拖累甚至回滚主流程。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long buyerId, OrderCreateDTO dto) {
        // ---------- 1. 归一化：合并同一商品的重复行 ----------
        // 前端可能传 [{productId:1,quantity:1},{productId:1,quantity:2}]，
        // 若不合并会对同一商品扣两次库存，导致版本号错乱、金额算错
        Map<Long, Integer> quantityByProduct = mergeItems(dto);

        // ---------- 2. 读取全部商品信息 ----------
        Map<Long, Product> productMap = new HashMap<>();
        for (Long productId : quantityByProduct.keySet()) {
            Product product = productMapper.findById(productId);
            if (product == null) {
                throw new BusinessException("商品不存在：" + productId);
            }
            // ---------- 3. 业务校验 ----------
            if (product.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException("商品「" + product.getTitle() + "」已下架");
            }
            if (product.getSellerId().equals(buyerId)) {
                throw new BusinessException("不能购买自己发布的商品：" + product.getTitle());
            }
            productMap.put(productId, product);
        }

        // ---------- 4. 乐观锁扣库存（按 productId 升序，降低死锁概率）----------
        List<Long> sortedIds = new ArrayList<>(quantityByProduct.keySet());
        sortedIds.sort(Comparator.naturalOrder());

        List<OrderItem> orderItems = new ArrayList<>(sortedIds.size());
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Long productId : sortedIds) {
            Product product = productMap.get(productId);
            int quantity = quantityByProduct.get(productId);

            // 提前校验只为给出友好提示，这一步不是并发安全的；
            // 真正的并发防线是下面 deductStock 的 WHERE 条件
            if (product.getStock() < quantity) {
                throw new StockNotEnoughException(409,
                        "商品「" + product.getTitle() + "」库存不足，仅剩 " + product.getStock() + " 件");
            }

            // version 传「读取时的版本号」，由 SQL 判断期间有没有被别人改过
            int rows = productMapper.deductStock(productId, quantity, product.getVersion());

            if (rows == 0) {
                // 影响 0 行 = version 已被别人改过，或库存不足。
                // 直接抛异常回滚，让用户重新下单。
                //
                // 【重要】绝不能在这里重查后再重试：
                // MySQL 默认 REPEATABLE READ 下，同一事务内的重查会读到旧快照，
                // version 永远对不上，会陷入死循环。
                log.warn("扣库存失败 productId={} quantity={} version={}",
                        productId, quantity, product.getVersion());
                throw new StockNotEnoughException(409,
                        "商品「" + product.getTitle() + "」已被其他用户抢先购买，请刷新后重试");
            }

            OrderItem item = new OrderItem();
            item.setProductId(productId);
            item.setQuantity(quantity);
            // 单价用读取时的实时价格，下单后不随商品调价而变动
            item.setPrice(product.getPrice());
            orderItems.add(item);

            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        // ---------- 5. 生成订单主记录 ----------
        Order order = new Order();
        order.setOrderNo(snowflakeIdGenerator.generateOrderNo());
        order.setBuyerId(buyerId);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(new Date());
        // pay_time 保持 null，支付时再填

        orderMapper.insert(order);
        // useGeneratedKeys 已把自增 id 回填到 order.id

        // ---------- 6. 批量插入订单项 ----------
        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
        }
        orderItemMapper.batchInsert(orderItems);

        log.info("下单成功 orderNo={} buyerId={} 商品数={} 总额={}",
                order.getOrderNo(), buyerId, orderItems.size(), totalPrice);

        return order.getOrderNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long buyerId, String orderNo) {
        Order order = requireOwnedOrder(buyerId, orderNo);

        if (order.getStatus() == OrderStatus.PAID) {
            return; // 幂等：已支付直接返回，不报错也不重复更新
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("订单已取消，无法支付");
        }

        // SQL 带 status='PENDING' 条件做双重保险：
        // 并发重复提交时第二次影响行数为 0，不会覆盖 pay_time
        int rows = orderMapper.pay(orderNo, new Date());
        if (rows == 0) {
            log.warn("支付时订单状态已变更 orderNo={}", orderNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long buyerId, String orderNo) {
        Order order = requireOwnedOrder(buyerId, orderNo);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return; // 幂等
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("订单已支付，不能取消");
        }

        if (orderMapper.cancel(orderNo) == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        // 回补库存。取消订单与回补库存在同一事务内，要么都成功要么都回滚。
        List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            throw new BusinessException("订单缺少订单项，数据异常：" + orderNo);
        }
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }

        log.info("订单已取消并回补库存 orderNo={}", orderNo);
    }

    @Override
    public OrderVO getDetail(Long userId, String orderNo) {
        OrderVO vo = orderMapper.findVOByOrderNo(orderNo);
        if (vo == null) {
            throw new BusinessException("订单不存在：" + orderNo);
        }
        // 隐私：订单详情仅买家本人可见。
        // 卖家通过 pageAsSeller 看列表（列表也含金额），这里不再开放单条详情。
        if (!vo.getBuyerId().equals(userId)) {
            throw new BusinessException(403, "无权查看该订单");
        }
        return vo;
    }

    @Override
    public PageResult<OrderVO> pageAsBuyer(Long buyerId, OrderQueryDTO query) {
        long total = orderMapper.countByBuyer(buyerId, query);
        List<OrderVO> list = total == 0 ? List.of() : orderMapper.findPageByBuyer(buyerId, query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), list);
    }

    @Override
    public PageResult<OrderVO> pageAsSeller(Long sellerId, OrderQueryDTO query) {
        long total = orderMapper.countBySeller(sellerId, query);
        List<OrderVO> list = total == 0 ? List.of() : orderMapper.findPageBySeller(sellerId, query);
        return PageResult.of(total, query.getPageNum(), query.getPageSize(), list);
    }

    // ==================== 私有方法 ====================

    /**
     * 合并同一商品的重复行，把数量累加。
     */
    private Map<Long, Integer> mergeItems(OrderCreateDTO dto) {
        Map<Long, Integer> merged = new HashMap<>();
        for (OrderCreateDTO.Item item : dto.getItems()) {
            merged.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return merged;
    }

    /**
     * 校验订单存在且属于该买家 —— 越权防线。
     */
    private Order requireOwnedOrder(Long buyerId, String orderNo) {
        Order order = orderMapper.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在：" + orderNo);
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(403, "无权操作该订单");
        }
        return order;
    }
}