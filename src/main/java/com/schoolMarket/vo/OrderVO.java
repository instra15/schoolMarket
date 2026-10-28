package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单出参对象。
 *
 * <p><b>一对多映射落点</b>：{@code items} 订单项列表由
 * {@code OrderMapper.xml} 的 {@code OrderVOMap} 用 {@code <collection>}
 * 一次查询带出，而不是在 Service 里再查一次订单项表再组装。</p>
 */
@Data
@Schema(description = "订单信息")
public class OrderVO {

    @Schema(description = "订单主键", example = "1")
    private Long id;

    @Schema(description = "订单号（雪花算法生成）", example = "SM1234567890")
    private String orderNo;

    @Schema(description = "买家ID", example = "1")
    private Long buyerId;

    @Schema(description = "订单总金额", example = "50.00")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态", example = "PENDING",
            allowableValues = {"PENDING", "PAID", "CANCELLED"})
    private String status;

    @Schema(description = "下单时间", example = "2026-10-13 19:30:00")
    private Date createTime;

    @Schema(description = "支付时间，未支付时为 null")
    private Date payTime;

    @Schema(description = "订单项列表（<collection> 一次带出）")
    private List<OrderItemVO> items;
}