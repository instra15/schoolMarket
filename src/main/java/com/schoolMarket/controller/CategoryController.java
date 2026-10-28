package com.schoolMarket.controller;

import com.schoolMarket.common.Result;
import com.schoolMarket.dto.CategoryCreateDTO;
import com.schoolMarket.service.CategoryService;
import com.schoolMarket.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类接口。
 *
 * <p>分类是全局共享的基础数据，增删改不校验登录身份
 * （个人练手项目够用；真实项目应限制为管理员）。</p>
 */
@Tag(name = "分类", description = "商品分类管理")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "全部分类", description = "附带每个分类下的商品数量")
    @GetMapping
    public Result<List<CategoryVO>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @Operation(summary = "分类详情")
    @GetMapping("/{id}")
    public Result<CategoryVO> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @Operation(summary = "新建分类", description = "分类名唯一")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryCreateDTO dto) {
        Long id = categoryService.create(dto);
        return Result.success("创建成功", id);
    }

    @Operation(summary = "编辑分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody CategoryCreateDTO dto) {
        categoryService.update(id, dto);
        return Result.success("修改成功", null);
    }

    @Operation(summary = "删除分类", description = "分类下仍有商品时拒绝删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success("删除成功", null);
    }
}