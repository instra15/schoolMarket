package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项出参对象。
 *
 * <p>Phase 3 使用：由 {@code OrderMapper.xml} 的 {@code OrderVOMap}
 * 用 {@code <collection>} 一次带出订单的所有订单项。</p>
 */
@Data
@Schema(description = "订单项信息")
public class OrderItemVO {

    @Schema(description = "订单项ID", example = "1")
    private Long id;

    @Schema(description = "商品ID", example = "1")
    private Long productId;

    @Schema(description = "商品标题（多表 JOIN 带出）", example = "数据结构教材（含笔记）")
    private String productTitle;

    @Schema(description = "商品主图（多表 JOIN 带出）")
    private String productImageUrl;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "下单时单价快照", example = "25.00")
    private BigDecimal price;

    @Schema(description = "小计 = 单价 × 数量", example = "50.00")
    public BigDecimal getSubtotal() {
        if (price == null || quantity == null) {
            return null;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}