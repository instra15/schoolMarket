package com.schoolMarket.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具。
 *
 * <p>采用 SHA-256 加盐哈希，不引入 Spring Security（计划书未提及，且引入会带进一整套过滤器链）。
 * 盐值固定为 {@code school_market_v1}。</p>
 *
 * <p>哈希算法：{@code SHA-256(明文 + ":" + 盐)}。
 * 校验种子数据中的哈希值：{@code printf '%s' "123456:school_market_v1" | sha256sum}</p>
 *
 * <p><b>安全说明</b>：SHA-256 属于快速哈希，抗暴力破解能力弱于 BCrypt/scrypt。
 * 本项目是个人练手项目、用于展示分层与校验流程，此处够用。
 * 若要用于真实生产，必须换成 BCrypt 或 scrypt（可引入 spring-security-crypto 单个依赖）。</p>
 */
public final class PasswordEncoder {

    private static final String SALT = "school_market_v1";

    private PasswordEncoder() {
    }

    /**
     * 计算密码哈希。
     */
    public static String encode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((rawPassword + ":" + SALT).getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 标准算法，理论上不可能走到这里
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    /**
     * 校验明文密码与哈希是否匹配。
     *
     * <p>用定长比较，避免因逐字符提前返回而泄露信息。</p>
     */
    public static boolean matches(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null) {
            return false;
        }
        return MessageDigest.isEqual(
                encode(rawPassword).getBytes(StandardCharsets.UTF_8),
                encoded.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}