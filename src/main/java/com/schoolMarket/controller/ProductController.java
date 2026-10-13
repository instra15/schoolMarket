package com.schoolMarket.controller;

import com.schoolMarket.annotation.CurrentUser;
import com.schoolMarket.common.PageResult;
import com.schoolMarket.common.Result;
import com.schoolMarket.dto.ProductCreateDTO;
import com.schoolMarket.dto.ProductQueryDTO;
import com.schoolMarket.dto.ProductUpdateDTO;
import com.schoolMarket.enums.ProductStatus;
import com.schoolMarket.service.ProductService;
import com.schoolMarket.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 商品模块接口。
 *
 * <p>所有写操作的当前用户ID 都由 {@code @CurrentUser} 从 Session 注入，
 * <b>不接受前端传入</b>，这是防止越权改别人商品的关键。</p>
 */
@Tag(name = "商品", description = "发布、编辑、上下架、搜索、详情")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "发布商品",
            description = "卖家ID 取自登录态；status 与 version 由服务端赋值，不接受前端传入")
    @PostMapping
    public Result<Long> publish(@CurrentUser Long userId,
                                @Valid @RequestBody ProductCreateDTO dto) {
        Long id = productService.publish(userId, dto);
        return Result.success("发布成功", id);
    }

    @Operation(summary = "编辑商品", description = "只能编辑自己的商品")
    @PutMapping("/{id}")
    public Result<Void> update(@CurrentUser Long userId,
                               @PathVariable Long id,
                               @Valid @RequestBody ProductUpdateDTO dto) {
        productService.update(userId, id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "上下架切换",
            description = "库存为 0 时不允许上架；重复设置相同状态会被拒绝")
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@CurrentUser Long userId,
                                     @PathVariable Long id,
                                     @Parameter(description = "目标状态", example = "OFF_SALE")
                                     @RequestParam ProductStatus status) {
        productService.changeStatus(userId, id, status);
        return Result.success("状态已更新", null);
    }

    @Operation(summary = "删除商品", description = "只能删除自己的商品，且库存必须为 0")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@CurrentUser Long userId, @PathVariable Long id) {
        productService.delete(userId, id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "分页搜索商品",
            description = "支持关键词、分类、价格区间、状态等动态条件；返回分类名与卖家昵称")
    @GetMapping
    public Result<PageResult<ProductVO>> search(@Valid ProductQueryDTO query) {
        return Result.success(productService.search(query));
    }

    @Operation(summary = "我的商品",
            description = "按登录用户过滤，忽略前端传入的 sellerId")
    @GetMapping("/mine")
    public Result<PageResult<ProductVO>> searchMine(@CurrentUser Long userId,
                                                    @Valid ProductQueryDTO query) {
        return Result.success(productService.searchMine(userId, query));
    }

    @Operation(summary = "商品详情", description = "一次 JOIN 带出分类名与卖家信息")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }
}