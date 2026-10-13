package com.schoolMarket.dto;

import com.schoolMarket.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单分页查询入参（买家视角与卖家视角通用）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单分页查询参数")
public class OrderQueryDTO extends PageQueryDTO {

    @Schema(description = "订单状态，不传则查全部",
            example = "PENDING", allowableValues = {"PENDING", "PAID", "CANCELLED"})
    private OrderStatus status;
}