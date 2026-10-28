package com.schoolMarket.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解，标注在 Controller 方法上。
 *
 * <p>用法：</p>
 * <pre>
 * &#64;LogOperation(action = "下单", targetType = "Order")
 * public Result&lt;String&gt; createOrder(...) { ... }
 * </pre>
 *
 * <p>由 {@link LogOperationAspect} 织入，方法正常返回与抛异常时各记一条日志。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 操作名称，如「下单」「发布商品」。
     */
    String action();

    /**
     * 目标类型，如 "Order"、"Product"。仅用于日志归类。
     */
    String targetType() default "";
}