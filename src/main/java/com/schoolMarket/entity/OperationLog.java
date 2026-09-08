package com.schoolMarket.entity;

//操作日志表

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationLog {

    private Long id;

    private User user;

    private String action;

    private String method;

    private String detail;

    private Date createTime;


}
