package com.schoolMarket.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>生成 64 位 long，结构：</p>
 * <pre>
 * 0 | 41 位时间戳差值 | 5 位 workerId | 5 位序列号
 *   |                    |              |
 *   |                    |              +-- 同一毫秒内自增，每毫秒上限 32 个
 *   |                    +-- 机器ID，0~31
 *   +-- 符号位恒为 0，保证生成的 ID 始终为正数
 * </pre>
 *
 * <p><b>为什么不用自增主键？</b>自增 ID 会暴露业务量（别人从你的 ID 就能猜出订单总数），
 * 且分库分表时无法全局唯一。雪花 ID 单调递增、全局唯一、不暴露业务量。</p>
 *
 * <p><b>时钟回拨</b>：检测到 now &lt; lastTimestamp 时自旋等待追上，而非抛异常。
 * 对个人练手项目足够；生产环境应采用带容错的方案（如美团 Leaf 的回拨容忍逻辑）。</p>
 *
 * <p>本类是 Spring 单例，{@link #generate()} 用 synchronized 保证线程安全。</p>
 */
@Component
public class SnowflakeIdGenerator {

    /** 起始时间戳：2024-01-01 00:00:00 UTC 的毫秒数 */
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 5L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 10

    private final long workerId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 单机部署，workerId 固定 0。
     */
    public SnowflakeIdGenerator() {
        this(0L);
    }

    public SnowflakeIdGenerator(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId 必须在 0~" + MAX_WORKER_ID + " 之间，当前=" + workerId);
        }
        this.workerId = workerId;
    }

    /**
     * 生成下一个 ID。线程安全。
     */
    public synchronized long generate() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨：等待时钟追上来
        if (timestamp < lastTimestamp) {
            timestamp = waitUntil(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号用尽，进入下一毫秒
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            // 新的一毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }

    /**
     * 生成字符串形式的订单号。
     *
     * <p>加 {@code SM}（SchoolMarket）前缀，排查问题时从日志一眼能认出这是订单号。</p>
     */
    public String generateOrderNo() {
        return "SM" + generate();
    }

    /**
     * 自旋等待直到系统时间超过 targetTimestamp。
     */
    private long waitUntil(long targetTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < targetTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}