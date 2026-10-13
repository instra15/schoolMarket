package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 商品分类出参对象。
 */
@Data
@Schema(description = "商品分类信息")
public class CategoryVO {

    @Schema(description = "分类ID", example = "1")
    private Long id;

    @Schema(description = "分类名", example = "教材书籍")
    private String name;

    @Schema(description = "分类描述", example = "课本、考研资料、习题集")
    private String description;

    @Schema(description = "该分类下的商品数量", example = "2")
    private Integer productCount;

    @Schema(description = "创建时间", example = "2026-10-13 16:30:00")
    private Date createTime;
}