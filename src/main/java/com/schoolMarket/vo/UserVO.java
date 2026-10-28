package com.schoolMarket.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户出参对象，已脱敏：<b>不含 password</b>。
 *
 * <p>VO 由 {@code UserMapper.xml} 的 {@code UserVOMap} 直接映射，
 * SQL 里只 SELECT 需要的列，从根源上杜绝密码泄露。</p>
 *
 * <p>原代码在 VO 上加了 {@code @NotBlank} 校验注解 —— 出参对象做入参校验没有意义，已删除。</p>
 */
@Data
@Schema(description = "用户信息（不含密码）")
public class UserVO {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "手机号", example = "13800138001")
    private String phone;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "注册时间", example = "2026-10-13 16:30:00")
    private Date createTime;
}