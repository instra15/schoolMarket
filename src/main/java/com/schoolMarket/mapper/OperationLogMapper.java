package com.schoolMarket.mapper;

import com.schoolMarket.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper。由 AOP 切面异步写入。
 */
@Mapper
public interface OperationLogMapper {

    /**
     * 新增操作日志。
     */
    int insert(OperationLog operationLog);

    /**
     * 分页查询某用户的操作日志。
     */
    List<OperationLog> findPageByUser(@Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    long countByUser(@Param("userId") Long userId);
}