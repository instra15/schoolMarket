package com.schoolMarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 异步通知实体，对应 notification 表。下单成功后由线程池异步写入。
 *
 * <p><b>字段命名注意</b>：这里刻意命名为 {@code read} 而不是 {@code isRead}。
 * Lombok 的 {@code @Data} 对 {@code boolean isRead} 生成的是 {@code isRead()} /
 * {@code setRead()}，MyBatis 的 PropertyNamer 会把 getter 名 {@code isRead}
 * 归一化成属性名 {@code read}，而数据库列 {@code is_read} 经驼峰转换是
 * {@code isRead} —— 两者不匹配，且<b>不会报任何错</b>，字段会永远读成 false。
 * 命名为 {@code read} 并在 resultMap 里显式映射 {@code <result column="is_read" property="read"/>}
 * 即可彻底避开这个坑。</p>
 */
@Data
public class Notification {

    private Long id;

    private Long userId;

    private String content;

    /** 对应数据库列 is_read */
    private Boolean read;

    private Date createTime;
}