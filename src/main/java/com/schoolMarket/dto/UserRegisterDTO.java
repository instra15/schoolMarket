package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册入参。
 */
@Data
@Schema(description = "用户注册参数")
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在 3~50 之间")
    @Schema(description = "用户名，登录账号", example = "wangwu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度需在 6~50 之间")
    @Schema(description = "密码，服务端加盐哈希后存储", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13900139003", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最长 50 个字符")
    @Schema(description = "昵称，对外展示", example = "王五", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;
}