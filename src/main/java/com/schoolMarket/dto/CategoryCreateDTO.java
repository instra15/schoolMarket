package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品分类新建 / 编辑入参。
 */
@Data
@Schema(description = "商品分类参数")
public class CategoryCreateDTO {

    @NotBlank(message = "分类名不能为空")
    @Size(max = 50, message = "分类名最长 50 个字符")
    @Schema(description = "分类名，全表唯一", example = "教材书籍", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 255, message = "分类描述最长 255 个字符")
    @Schema(description = "分类描述", example = "课本、考研资料、习题集")
    private String description;
}