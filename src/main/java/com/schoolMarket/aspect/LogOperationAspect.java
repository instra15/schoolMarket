package com.schoolMarket.aspect;

import com.schoolMarket.entity.OperationLog;
import com.schoolMarket.mapper.OperationLogMapper;
import com.schoolMarket.util.TransactionAfterCommitExecutor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

/**
 * 操作日志切面。
 *
 * <p>用 {@code @AfterReturning} / {@code @AfterThrowing} 两个后置通知，
 * 分别记录成功与失败。相比 {@code @Around} 的好处：<b>不改变原方法的返回值与控制流</b>。</p>
 *
 * <p><b>关于 userId</b>：切面里拿不到 {@code @CurrentUser} 注入的值
 * （那是 Controller 参数绑定，不是 ThreadLocal），
 * 所以通过 {@link RequestContextHolder} 取当前请求，再从 Session 拿 userId。</p>
 *
 * <p><b>关键约束</b>：日志写入走异步，且挂在 afterCommit 上。
 * 若同步写，操作日志会成为下单接口的性能瓶颈；
 * 若与主流程同事务，日志写失败会回滚下单 —— 两者都不合适。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogOperationAspect {

    private static final String SESSION_USER_ID = "userId";
    private static final int MAX_DETAIL_LENGTH = 500;

    private final OperationLogMapper operationLogMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    /**
     * 切点：所有标注了 {@link LogOperation} 的方法。
     */
    @Pointcut("@annotation(com.schoolMarket.aspect.LogOperation)")
    public void logOperationPointcut() {
    }

    /**
     * 方法正常返回后记录成功日志。
     */
    @AfterReturning(pointcut = "logOperationPointcut()", returning = "result")
    public void logSuccess(JoinPoint joinPoint, LogOperation logOp, Object result) {
        saveAsync(joinPoint, logOp, buildDetail(logOp, result, "SUCCESS"), "SUCCESS");
    }

    /**
     * 方法抛异常后记录失败日志。
     *
     * <p>{@code @AfterThrowing} <b>不会吞掉异常</b>，
     * 异常继续抛给 GlobalExceptionHandler —— 这是想要的行为，
     * 切面只观察，不干预控制流。</p>
     */
    @AfterThrowing(pointcut = "logOperationPointcut()", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, LogOperation logOp, Exception ex) {
        saveAsync(joinPoint, logOp, buildDetail(logOp, null, "FAILED: " + ex.getMessage()), "FAILED");
    }

    /**
     * 组装日志 detail，并截断到 varchar(500) 以内。
     */
    private String buildDetail(LogOperation logOp, Object result, String status) {
        StringBuilder sb = new StringBuilder();
        if (logOp.targetType() != null && !logOp.targetType().isBlank()) {
            sb.append('[').append(logOp.targetType()).append("] ");
        }
        sb.append(status);
        if (result != null) {
            sb.append(" | 返回: ").append(truncate(String.valueOf(result)));
        }
        return truncate(sb.toString());
    }

    /**
     * 截断超长文本，防止撑爆 varchar(500)。
     */
    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_DETAIL_LENGTH
                ? text.substring(0, MAX_DETAIL_LENGTH - 3) + "..."
                : text;
    }

    /**
     * 取目标方法签名，形如 {@code OrderController#createOrder}。
     */
    private String methodSignature(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            return signature.getDeclaringType().getSimpleName() + "#" + signature.getName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 异步写日志。切面自身绝不能抛异常，否则会影响业务执行。
     */
    private void saveAsync(JoinPoint joinPoint, LogOperation logOp, String detail, String result) {
        try {
            Long userId = currentUserId();
            String method = methodSignature(joinPoint);
            String action = logOp.action();

            afterCommitExecutor.executeAfterCommit(() -> {
                try {
                    OperationLog operationLog = new OperationLog();
                    operationLog.setUserId(userId);
                    operationLog.setAction(action);
                    operationLog.setMethod(method);
                    operationLog.setDetail(detail);
                    operationLog.setResult(result);
                    operationLog.setCreateTime(new Date());
                    operationLogMapper.insert(operationLog);
                } catch (Exception e) {
                    log.error("写操作日志失败 action={} method={}", action, method, e);
                }
            });
        } catch (Exception e) {
            // 兜底：切面绝不能影响业务
            log.error("记录操作日志时出错", e);
        }
    }

    /**
     * 通过 RequestContextHolder 取当前登录用户ID。
     *
     * <p>非 Web 线程（定时任务等）或未登录时返回 null —— 这是正常的，
     * 日志表 user_id 允许为空。</p>
     */
    private Long currentUserId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);
            if (session == null) {
                return null;
            }
            Object userId = session.getAttribute(SESSION_USER_ID);
            return userId instanceof Number number ? number.longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }
}