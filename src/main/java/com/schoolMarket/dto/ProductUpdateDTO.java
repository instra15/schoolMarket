package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品编辑入参。
 *
 * <p>与 {@link ProductCreateDTO} 的区别：不包含 status（上下架走专门接口），
 * 也不包含 version（乐观锁版本号由服务端在 SQL 里自增，绝不能让前端传）。</p>
 */
@Data
@Schema(description = "商品编辑参数")
public class ProductUpdateDTO {

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题最长 100 个字符")
    @Schema(description = "商品标题", example = "数据结构教材（含笔记）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 500, message = "商品描述最长 500 个字符")
    @Schema(description = "商品描述", example = "九成新，无划线")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    @Schema(description = "单价", example = "25.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    @Schema(description = "库存数量", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer stock;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;
}