package com.schoolMarket.mapper;

import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.entity.User;
import com.schoolMarket.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper。
 *
 * <p>SQL 全部写在 {@code resources/mapper/UserMapper.xml} 中，
 * 本接口只声明方法 —— 这是 MyBatis 推荐的用法，
 * 动态 SQL 和多表关联放在 XML 里可读性远好于字符串拼接。</p>
 *
 * <p>注意表名 {@code user} 是 MySQL 关键字，XML 中一律写成 {@code `user`}。</p>
 */
@Mapper
public interface UserMapper {

    /**
     * 按用户名查（含密码哈希，登录校验用）。
     */
    User findByUsername(@Param("username") String username);

    /**
     * 按主键查（含密码哈希）。
     */
    User findById(@Param("id") Long id);

    /**
     * 查用户信息并映射为脱敏 VO（不含密码），SQL 中不 SELECT password 列。
     */
    UserVO findVOById(@Param("id") Long id);

    /**
     * 查用户信息并映射为脱敏 VO，按用户名定位。
     */
    UserVO findVOByUsername(@Param("username") String username);

    /**
     * 新增用户。密码哈希由 Service 层算好传入。
     */
    int insert(User user);

    /**
     * 更新昵称与手机号。
     *
     * @return 影响行数
     */
    int updateProfile(@Param("username") String username,
                      @Param("nickname") String nickname,
                      @Param("phone") String phone);

    /**
     * 更新密码哈希。
     */
    int updatePassword(@Param("username") String username,
                       @Param("password") String password);
}