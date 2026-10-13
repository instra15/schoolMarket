package com.schoolMarket.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应体。
 *
 * <p>分页方案：手写 SQL 的 {@code LIMIT #{pageSize} OFFSET #{offset}}，
 * 不引入 PageHelper。偏移量由 {@link com.schoolMarket.dto.PageQueryDTO#getOffset()} 计算。</p>
 */
@Data
@Schema(description = "分页响应体")
public class PageResult<T> implements Serializable {

    @Schema(description = "总记录数", example = "57")
    private long total;

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private int pageNum;

    @Schema(description = "每页条数", example = "10")
    private int pageSize;

    @Schema(description = "当前页数据")
    private List<T> list;

    /**
     * 分页结果工厂方法，pageNum 从 1 开始。
     */
    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> list) {
        PageResult<T> page = new PageResult<>();
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setList(list == null ? Collections.emptyList() : list);
        return page;
    }
}