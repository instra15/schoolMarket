package com.schoolMarket.exception;

import com.schoolMarket.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>HTTP 状态码约定：响应体的 code 与 HTTP 状态码保持一致，
 * 不用「HTTP 200 + body 里 code=500」那套混合风格。</p>
 *
 * <table border="1">
 *   <tr><th>异常</th><th>HTTP</th><th>含义</th></tr>
 *   <tr><td>StockNotEnoughException</td><td>409</td><td>库存不足或乐观锁冲突（资源状态冲突）</td></tr>
 *   <tr><td>BusinessException</td><td>400</td><td>业务规则错误（用户名重复、状态不对…）</td></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>@Valid 校验失败</td></tr>
 *   <tr><td>BindException</td><td>400</td><td>表单/查询参数绑定校验失败</td></tr>
 *   <tr><td>ConstraintViolationException</td><td>400</td><td>@Validated 方法级参数校验失败</td></tr>
 *   <tr><td>HandlerMethodValidationException</td><td>400</td><td>Spring 6.1+ 内置方法参数校验失败</td></tr>
 *   <tr><td>ServletRequestBindingException</td><td>401</td><td>未登录（Session 中取不到 userId）</td></tr>
 *   <tr><td>Exception</td><td>500</td><td>兜底，异常信息不外泄</td></tr>
 * </table>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 乐观锁冲突 / 库存不足 —— 用 409 而非 400，
     * 因为这是「资源状态冲突」而非「参数错误」。
     */
    @ExceptionHandler(StockNotEnoughException.class)
    public ResponseEntity<Result<Void>> handleStock(StockNotEnoughException e) {
        return build(HttpStatus.valueOf(e.getCode()), e.getMessage());
    }

    /**
     * 业务异常。状态码由异常自带的 code 决定（默认 400，
     * {@code @CurrentUser} 未登录时抛的是 401）。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return build(HttpStatus.valueOf(e.getCode()), e.getMessage());
    }

    /**
     * {@code @Valid} 校验 {@code @RequestBody} 失败。把所有字段错误拼成一条便于前端展示。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg.isEmpty() ? "参数绑定失败" : msg);
    }

    /**
     * {@code @Validated} 打在方法参数上时的校验失败（老式写法）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, msg.isEmpty() ? "参数校验失败" : msg);
    }

    /**
     * Spring 6.1+ 内置的 HandlerMethod 校验失败。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Result<Void>> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.debug("方法参数校验失败: {}", e.getMessage());
        return build(HttpStatus.BAD_REQUEST, "参数校验失败");
    }

    /**
     * {@code @SessionAttribute} 取不到值等绑定失败 —— 统一按未登录处理。
     * 这正是「未登录调用 /me 返回 401」的来源。
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Result<Void>> handleBinding(ServletRequestBindingException e) {
        log.debug("参数绑定失败: {}", e.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
    }

    /**
     * 兜底：异常堆栈只写日志，不返回给前端，避免泄露内部信息。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("系统异常", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Result.error(status.value(), message));
    }
}