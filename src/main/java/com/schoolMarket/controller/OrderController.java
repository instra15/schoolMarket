package com.schoolMarket.controller;

import com.schoolMarket.annotation.CurrentUser;
import com.schoolMarket.common.PageResult;
import com.schoolMarket.common.Result;
import com.schoolMarket.dto.OrderCreateDTO;
import com.schoolMarket.dto.OrderQueryDTO;
import com.schoolMarket.service.OrderService;
import com.schoolMarket.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单模块接口 —— 项目的并发控制核心。
 *
 * <p>所有涉及身份的接口都用 {@code @CurrentUser} 从Session 取 userId，
 * 不接受前端传 buyerId / sellerId。</p>
 */
@Tag(name = "订单", description = "下单、支付、取消、查询（含乐观锁防超卖）")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单",
            description = "扣库存 + 生成订单 + 生成订单项在同一事务内；"
                    + "使用乐观锁 version 防超卖，库存不足或被抢先时返回 409")
    @PostMapping
    public Result<String> createOrder(@CurrentUser Long buyerId,
                                      @Valid @RequestBody OrderCreateDTO dto) {
        String orderNo = orderService.createOrder(buyerId, dto);
        return Result.success("下单成功", orderNo);
    }

    @Operation(summary = "模拟支付",
            description = "幂等操作：已支付时重复调用也返回成功，不会重复更新支付时间")
    @PutMapping("/{orderNo}/pay")
    public Result<Void> pay(@CurrentUser Long buyerId,
                            @Parameter(description = "订单号", example = "SM1234567890")
                            @PathVariable String orderNo) {
        orderService.pay(buyerId, orderNo);
        return Result.success("支付成功", null);
    }

    @Operation(summary = "取消订单",
            description = "幂等操作；取消后自动回补已扣的库存")
    @PutMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@CurrentUser Long buyerId, @PathVariable String orderNo) {
        orderService.cancel(buyerId, orderNo);
        return Result.success("订单已取消", null);
    }

    @Operation(summary = "订单详情",
            description = "含订单项列表（<collection> 一次 JOIN 带出）；仅买家本人可见")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@CurrentUser Long userId, @PathVariable String orderNo) {
        return Result.success(orderService.getDetail(userId, orderNo));
    }

    @Operation(summary = "我买到的订单", description = "买家视角分页查询")
    @GetMapping
    public Result<PageResult<OrderVO>> myOrders(@CurrentUser Long buyerId,
                                                @Valid OrderQueryDTO query) {
        return Result.success(orderService.pageAsBuyer(buyerId, query));
    }

    @Operation(summary = "我卖出的订单",
            description = "卖家视角分页查询；order 表不存 seller_id，需 JOIN 订单项与商品表")
    @GetMapping("/sold")
    public Result<PageResult<OrderVO>> soldOrders(@CurrentUser Long sellerId,
                                                  @Valid OrderQueryDTO query) {
        // 注意：此路径与 GET /{orderNo} 同级，Spring 路径匹配是「最精确优先」，
        // 字面量路径 /sold 会优先于路径变量 /{orderNo} 命中，不会被吞掉。
        return Result.success(orderService.pageAsSeller(sellerId, query));
    }
}