package com.schoolMarket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码入参，必须提供旧密码做校验。
 */
@Data
@Schema(description = "修改密码参数")
public class UserPasswordDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "原密码不能为空")
    @Schema(description = "原密码，用于身份校验", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度需在 6~50 之间")
    @Schema(description = "新密码", example = "654321", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}