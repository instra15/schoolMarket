package com.schoolMarket;

import com.schoolMarket.support.RequiresDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用上下文加载测试。
 *
 * <p>验证整个 Spring 容器能正常启动：配置绑定、Bean 装配、
 * MyBatis Mapper 扫描、AOP 切面注册、线程池初始化都不出错。</p>
 *
 * <p>加了 {@link RequiresDatabase} —— 没配数据库密码时自动跳过，
 * 避免开发者本机没数据库时 {@code mvn test} 直接失败。</p>
 */
@RequiresDatabase
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("应用上下文加载测试")
class SchoolMarketApplicationTests {

    @Test
    @DisplayName("Spring 上下文能正常启动")
    void contextLoads() {
        // 能走到这里就说明容器启动成功了
    }
}
