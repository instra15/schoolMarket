package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询入参基类。
 *
 * <p>Phase 1 先建好，Phase 2 的商品分页搜索会继承并追加 keyword / categoryId / 价格区间等条件。</p>
 */
@Data
@Schema(description = "分页查询参数")
public class PageQueryDTO {

    @Min(value = 1, message = "页码最小为 1")
    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页最多 100 条")
    @Schema(description = "每页条数", example = "10", defaultValue = "10")
    private Integer pageSize = 10;

    /**
     * LIMIT 的偏移量，供 Mapper 的 {@code OFFSET #{offset}} 使用。
     */
    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}