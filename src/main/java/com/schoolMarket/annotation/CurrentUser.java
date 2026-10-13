package com.schoolMarket.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 注入当前登录用户ID。
 *
 * <p>用法：</p>
 * <pre>
 * public Result&lt;Void&gt; publish(@CurrentUser Long userId, @Valid @RequestBody ProductCreateDTO dto)
 * </pre>
 *
 * <p>相比 Phase 1 用的 {@code @SessionAttribute("userId")}，好处是不用在每个方法里
 * 重复写字符串常量，且「这个参数不是客户端传的」这件事在签名上一目了然。</p>
 *
 * <p>由 {@link com.schoolMarket.config.CurrentUserResolver} 解析。</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * 是否强制要求已登录。
     *
     * <p>true（默认）：未登录时抛 BusinessException，全局处理器转 401。</p>
     * <p>false：未登录时注入 null，由业务代码自行决定怎么处理。</p>
     */
    boolean required() default true;
}