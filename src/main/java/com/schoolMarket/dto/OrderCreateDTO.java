package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单入参。
 *
 * <p>相比原版的改动：下单只提交「要买什么」，订单号、总价、买家、状态
 * 全部由服务端根据登录用户与商品实时价格生成，绝不能让前端传入。</p>
 */
@Data
@Schema(description = "下单参数")
public class OrderCreateDTO {

    @NotEmpty(message = "订单项不能为空")
    @Valid
    @Schema(description = "订单项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Data
    @Schema(description = "单个订单项")
    public static class Item {

        @NotNull(message = "商品ID不能为空")
        @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于 0")
        @Schema(description = "购买数量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer quantity;
    }
}