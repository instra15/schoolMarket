package com.schoolMarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 操作日志实体，对应 operation_log 表。由 Phase 4 的 AOP 切面异步写入。
 */
@Data
public class OperationLog {

    private Long id;

    /** 操作用户ID，可为空（如注册接口） */
    private Long userId;

    /** 操作名称，如「下单」「发布商品」 */
    private String action;

    /** 目标方法签名 */
    private String method;

    /** 操作描述 / 业务标识 */
    private String detail;

    /** 执行结果 SUCCESS / FAILED: xxx */
    private String result;

    private Date createTime;
}