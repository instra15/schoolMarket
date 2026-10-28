package com.schoolMarket.controller;

import com.schoolMarket.annotation.CurrentUser;
import com.schoolMarket.common.PageResult;
import com.schoolMarket.common.Result;
import com.schoolMarket.dto.PageQueryDTO;
import com.schoolMarket.service.NotificationService;
import com.schoolMarket.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口。
 *
 * <p>通知由下单流程异步写入，这里只负责查询与标记已读。</p>
 */
@Tag(name = "通知", description = "异步通知的查询与已读标记")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的通知",
            description = "分页查询；可用 read 参数筛选已读/未读，不传则查全部")
    @GetMapping
    public Result<PageResult<NotificationVO>> list(
            @CurrentUser Long userId,
            @Valid PageQueryDTO query,
            @Parameter(description = "是否已读，不传则查全部", example = "false")
            @RequestParam(required = false) Boolean read) {
        return Result.success(notificationService.pageMine(userId, query, read));
    }

    @Operation(summary = "未读数量", description = "用于消息红点")
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@CurrentUser Long userId) {
        return Result.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记已读", description = "幂等；只能标记自己的通知")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@CurrentUser Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return Result.success("已标记为已读", null);
    }

    @Operation(summary = "全部标记已读", description = "返回本次实际标记的条数")
    @PutMapping("/read-all")
    public Result<Integer> markAllRead(@CurrentUser Long userId) {
        int count = notificationService.markAllRead(userId);
        return Result.success("已全部标记为已读", count);
    }
}