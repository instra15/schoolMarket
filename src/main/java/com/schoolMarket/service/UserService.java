package com.schoolMarket.service;

import com.schoolMarket.dto.UserLoginDTO;
import com.schoolMarket.dto.UserPasswordDTO;
import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.dto.UserUpdateDTO;
import com.schoolMarket.vo.UserVO;

/**
 * 用户服务。
 *
 * <p>Service 返回业务数据对象（UserVO）或 void，不返回 {@code Result} 包装体 ——
 * 响应包装统一由 Controller 的返回类型和全局异常处理负责。
 * 业务失败通过抛出 {@code BusinessException} 表达，由 {@code GlobalExceptionHandler}
 * 转成对应 HTTP 状态码。这比在 Service 里 return 错误响应体更清晰。</p>
 */
public interface UserService {

    /**
     * 注册新用户，用户名重复时抛 BusinessException。
     *
     * @return 新用户ID
     */
    Long register(UserRegisterDTO dto);

    /**
     * 登录，校验密码。失败时抛 BusinessException。
     *
     * @return 用户ID（供 Controller 写入 Session）
     */
    UserVO login(UserLoginDTO dto);

    /**
     * 按ID查询用户信息（脱敏）。
     */
    UserVO getById(Long id);

    /**
     * 按用户名查询用户信息（脱敏）。
     */
    UserVO getByUsername(String username);

    /**
     * 修改昵称与手机号。
     */
    void updateProfile(UserUpdateDTO dto);

    /**
     * 修改密码，需校验旧密码。新旧密码相同时抛 BusinessException。
     */
    void updatePassword(UserPasswordDTO dto);
}