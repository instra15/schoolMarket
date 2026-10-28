package com.schoolMarket.config;

import com.schoolMarket.annotation.CurrentUser;
import com.schoolMarket.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentUser} 注解的参数解析器。
 *
 * <p>从 Session 里取出 userId 注入到 Controller 方法参数。
 * 没有这一步，Spring 不认识 {@code @CurrentUser}，启动时会报
 * 「No suitable resolver found」。</p>
 *
 * <p><b>注意 getSession(false)</b>：不传 false 会为每个未登录请求都创建一个新 Session，
 * 白白产生 JSESSIONID。这个参数表示「只在已有 Session 时才获取」。</p>
 */
@Slf4j
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    /** Session 中用户ID的键名，与 UserController 登录时写入的一致 */
    private static final String SESSION_USER_ID = "userId";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && (Long.class.equals(parameter.getParameterType())
                    || long.class.equals(parameter.getParameterType()));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        CurrentUser annotation = parameter.getParameterAnnotation(CurrentUser.class);

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpSession session = request == null ? null : request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(SESSION_USER_ID);

        if (userId == null) {
            if (annotation.required()) {
                log.debug("未登录访问需要身份的接口: {}",
                        parameter.getMethod() == null ? "?" : parameter.getMethod().getName());
                throw new BusinessException(401, "未登录或登录已过期");
            }
            return null;
        }
        return userId;
    }
}