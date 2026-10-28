package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品出参对象。
 *
 * <p><b>关联映射落点</b>：{@code category} 与 {@code seller} 两个嵌套对象
 * 由 {@code ProductMapper.xml} 的 {@code ProductVOMap} 用 {@code <association>}
 * 一次 JOIN 带出，避免在 Service 里手动查两次再组装（N+1 问题）。</p>
 *
 * <p>{@code status} 存成 String 而非 {@code ProductStatus} 枚举：
 * 枚举序列化在 Swagger 上展示为对象名不如字符串直观，且前端只需展示、不需判断。</p>
 */
@Data
@Schema(description = "商品信息")
public class ProductVO {

    @Schema(description = "商品ID", example = "1")
    private Long id;

    @Schema(description = "商品标题", example = "数据结构教材（含笔记）")
    private String title;

    @Schema(description = "商品描述", example = "九成新，无划线")
    private String description;

    @Schema(description = "商品主图地址")
    private String imageUrl;

    @Schema(description = "单价", example = "25.00")
    private BigDecimal price;

    @Schema(description = "剩余库存", example = "5")
    private Integer stock;

    @Schema(description = "状态", example = "ON_SALE", allowableValues = {"ON_SALE", "OFF_SALE"})
    private String status;

    @Schema(description = "分类信息")
    private CategoryVO category;

    @Schema(description = "卖家信息（不含密码）")
    private UserVO seller;

    @Schema(description = "上架时间", example = "2026-10-13 16:30:00")
    private Date createTime;

    @Schema(description = "更新时间", example = "2026-10-13 16:30:00")
    private Date updateTime;
}