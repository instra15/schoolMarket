package com.schoolMarket.entity;

//异步通知表

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    private Long id;

    private User user;

    private String content;

    private boolean isRead;

    private Date createTime;

}
