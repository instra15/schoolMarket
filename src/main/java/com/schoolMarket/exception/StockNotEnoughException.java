package com.schoolMarket.exception;

/**
 * 库存不足 / 乐观锁冲突。
 *
 * <p>默认映射为 HTTP 409 Conflict —— 这是「资源状态冲突」而非「参数错误」，
 * 前端拿到 409 应该提示用户刷新重试，而不是让他改表单。</p>
 */
public class StockNotEnoughException extends RuntimeException {

    private final int code;

    public StockNotEnoughException() {
        this.code = 409;
    }

    public StockNotEnoughException(String message) {
        super(message);
        this.code = 409;
    }

    public StockNotEnoughException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}