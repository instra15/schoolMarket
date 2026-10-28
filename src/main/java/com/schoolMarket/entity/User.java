package com.schoolMarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 用户实体，对应 user 表。
 *
 * <p>注意：password 仅供内部使用，禁止通过 VO 直接透出给前端。
 * VO 转换由 {@code UserMapper.xml} 的 resultMap 完成，不在实体里写转换方法。</p>
 */
@Data
public class User {

    private Long id;

    /** 登录账号，全表唯一 */
    private String username;

    /** SHA-256 加盐哈希，绝不是明文 */
    private String password;

    private String phone;

    /** 昵称，对外展示 */
    private String nickname;

    private Date createTime;
}