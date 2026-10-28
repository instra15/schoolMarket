package com.schoolMarket.dto;

import com.schoolMarket.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品分页搜索入参。
 *
 * <p>继承 {@link PageQueryDTO} 拿到 pageNum / pageSize / offset，
 * 这里追加 5 个动态查询条件，对应 XML 里的一组 {@code <if>}：</p>
 *
 * <ul>
 *   <li>{@code keyword}   → 标题或描述模糊匹配</li>
 *   <li>{@code categoryId}→ 精确匹配分类</li>
 *   <li>{@code minPrice} / {@code maxPrice} → 价格区间</li>
 *   <li>{@code status}    → 精确匹配状态</li>
 * </ul>
 *
 * <p>所有字段都可为空，为空则该条件不参与拼接 —— 这正是 MyBatis 动态 SQL 的意义。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品分页搜索参数")
public class ProductQueryDTO extends PageQueryDTO {

    @Schema(description = "关键词，对标题与描述做模糊匹配", example = "教材")
    private String keyword;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @DecimalMin(value = "0.00", message = "最低价格不能为负数")
    @Schema(description = "价格下限（含）", example = "0")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.00", message = "最高价格不能为负数")
    @Schema(description = "价格上限（含）", example = "200")
    private BigDecimal maxPrice;

    @Schema(description = "商品状态", example = "ON_SALE", allowableValues = {"ON_SALE", "OFF_SALE"})
    private ProductStatus status;

    @Schema(description = "只看该卖家的商品（内部接口用）", example = "1")
    private Long sellerId;
}