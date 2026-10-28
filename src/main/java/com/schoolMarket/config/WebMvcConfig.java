package com.schoolMarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 配置。
 *
 * <p><b>不要在这个类里配 Jackson</b>：Spring Boot 4 用的是 Jackson 3
 * （tools.jackson），经典的 {@code Jackson2ObjectMapperBuilderCustomizer}
 * 类已不存在，写了会编译失败。日期格式统一在 application.yaml 里配。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserResolver currentUserResolver;

    /**
     * 注册自定义参数解析器，让 {@code @CurrentUser} 生效。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserResolver);
    }
}