package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 通知出参对象。
 */
@Data
@Schema(description = "通知信息")
public class NotificationVO {

    @Schema(description = "通知ID", example = "1")
    private Long id;

    @Schema(description = "通知内容", example = "下单成功：SM123456789")
    private String content;

    @Schema(description = "是否已读", example = "false")
    private Boolean read;

    @Schema(description = "创建时间", example = "2026-10-20 14:00:00")
    private Date createTime;
}