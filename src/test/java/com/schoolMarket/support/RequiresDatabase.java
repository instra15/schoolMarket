package com.schoolMarket.support;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要真实数据库的测试标记。
 *
 * <p><b>为什么需要它</b>：本项目的 Mapper 测试与并发测试都必须连真实 MySQL
 * （{@code @MybatisTest} 不支持 H2 这类内存库替代 —— 我们的 SQL 用了
 * MySQL 专有的 {@code CONCAT}、反引号、{@code LIMIT ... OFFSET} 等语法）。
 * 如果开发者本机没配数据库，{@code mvn test} 就会一片红。
 * </p>
 *
 * <p>加上本注解后，未设置 {@code DATABASE_PASSWORD} 环境变量的测试会被
 * <b>自动跳过</b>（JUnit 报 skipped 而非 failed），CI 与新同事克隆下来
 * 都能干净地执行 {@code mvn test}。</p>
 *
 * <p><b>运行方式</b>：</p>
 * <pre>
 * # 跳过所有需要数据库的测试（默认行为）
 * mvn test
 *
 * # 配置密码后跑全部测试
 * export DATABASE_PASSWORD='你的MySQL密码'
 * mvn test
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "DATABASE_PASSWORD", matches = ".+",
        disabledReason = "未设置 DATABASE_PASSWORD 环境变量，跳过需要真实数据库的测试")
public @interface RequiresDatabase {
}
