package com.schoolMarket.controller;

import com.schoolMarket.common.Result;
import com.schoolMarket.dto.UserLoginDTO;
import com.schoolMarket.dto.UserPasswordDTO;
import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.dto.UserUpdateDTO;
import com.schoolMarket.service.UserService;
import com.schoolMarket.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户模块接口。
 *
 * <p>登录态说明：用 Spring 内置的 {@link org.springframework.web.bind.annotation.SessionAttribute}
 * 读取 Session 中的 userId，零自定义代码。取不到值时会抛
 * {@code ServletRequestBindingException}，由 {@code GlobalExceptionHandler} 转成 401。</p>
 *
 * <p>Phase 2 会把这套替换为自定义的 {@code @CurrentUser} 注解 + 参数解析器，
 * 届时业务代码只需改注解、调用方式不变。</p>
 */
@Tag(name = "用户", description = "注册、登录、个人信息")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    /** Session 中存放当前登录用户ID的键名 */
    public static final String SESSION_USER_ID = "userId";

    private final UserService userService;

    @Operation(summary = "注册用户",
            description = "用户名唯一，密码经 SHA-256 加盐哈希后存储")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterDTO dto) {
        Long id = userService.register(dto);
        return Result.success("注册成功", id);
    }

    @Operation(summary = "登录",
            description = "成功后把 userId 写入 Session，后续需携带 Cookie")
    @PostMapping("/login")
    public Result<Long> login(@Valid @RequestBody UserLoginDTO dto, HttpSession session) {
        UserVO vo = userService.login(dto);
        session.setAttribute(SESSION_USER_ID, vo.getId());
        return Result.success("登录成功", vo.getId());
    }

    @Operation(summary = "查询当前登录用户",
            description = "从 Session 取 userId，未登录返回 401")
    @GetMapping("/me")
    public Result<UserVO> me(
            @Parameter(description = "当前登录用户ID，从 Session 自动注入")
            @SessionAttribute(SESSION_USER_ID) Long userId) {
        return Result.success(userService.getById(userId));
    }

    @Operation(summary = "按ID查询用户", description = "返回脱敏信息，不含密码")
    @GetMapping("/{id}")
    public Result<UserVO> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @Operation(summary = "修改个人资料", description = "仅可修改昵称与手机号")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success("资料已更新", null);
    }

    @Operation(summary = "修改密码", description = "需提供正确的原密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UserPasswordDTO dto) {
        userService.updatePassword(dto);
        return Result.success("密码已修改", null);
    }

    @Operation(summary = "退出登录", description = "清除 Session")
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_ID);
        return Result.success("已退出登录", null);
    }
}