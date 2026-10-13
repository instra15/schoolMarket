package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料入参。用户名与密码不可通过此接口修改。
 */
@Data
@Schema(description = "修改个人资料参数")
public class UserUpdateDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名（作为定位条件，不允许改成其他值）", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "新手机号", example = "13900139004", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50 个字符")
    @Schema(description = "新昵称", example = "张三丰", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
}