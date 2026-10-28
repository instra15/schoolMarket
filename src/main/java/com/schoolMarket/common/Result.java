package com.schoolMarket.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。
 *
 * <p>约定：响应体的 code 与 HTTP 状态码保持一致，
 * 不使用「HTTP 200 + body 里 code=500」这种混合风格，
 * 这样 curl -i 和 Swagger UI 都能正确反映结果。</p>
 */
@Data
@Schema(description = "统一响应体")
public class Result<T> implements Serializable {

    @Schema(description = "状态码，与 HTTP 状态码一致：200成功 / 400参数或业务错误 / 401未登录 / 409冲突 / 500系统异常",
            example = "200")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    public static <T> Result<T> success() {
        return build(200, "success", null);
    }

    public static <T> Result<T> success(T data) {
        return build(200, "success", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return build(200, message, data);
    }

    public static <T> Result<T> error(String message) {
        return build(500, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return build(code, message, null);
    }

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}