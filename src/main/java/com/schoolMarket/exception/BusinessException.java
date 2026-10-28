package com.schoolMarket.exception;

/**
 * 业务异常基类。
 *
 * <p>带 HTTP 状态码：默认 400，需要特定状态码（如 401 未登录）时用双参构造器。
 * 最终由 {@link GlobalExceptionHandler} 转成对应 HTTP 响应。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException() {
        this.code = 400;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}