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
 * 商品发布入参。
 *
 * <p>相比原版的改动：</p>
 * <ul>
 *   <li>{@code Category category} → {@code Long categoryId}：外键用 ID 表达，
 *       不接受嵌套对象（否则 MyBatis 无法绑定到 category_id 列）</li>
 *   <li>删除 {@code version}：乐观锁版本号必须由服务端管理，绝不能让前端传</li>
 *   <li>删除 {@code status}：上架初始状态固定为 ON_SALE，
 *       上下架应由专门接口操作（Phase 2 补）</li>
 * </ul>
 */
@Data
@Schema(description = "商品发布参数")
public class ProductCreateDTO {

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题最长 100 个字符")
    @Schema(description = "商品标题", example = "数据结构教材（含笔记）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 500, message = "商品描述最长 500 个字符")
    @Schema(description = "商品描述", example = "九成新，无划线，附赠思维导图")
    private String description;

    @Size(max = 255, message = "图片地址最长 255 个字符")
    @Schema(description = "商品主图地址", example = "https://example.com/book.jpg")
    private String imageUrl;

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