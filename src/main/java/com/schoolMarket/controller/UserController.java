package com.schoolMarket.controller;

import com.schoolMarket.common.Response;
import com.schoolMarket.dto.UserDTO;
import com.schoolMarket.service.UserService;
import com.schoolMarket.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Register user")
    @PostMapping("/register")
    public Response<Void> register(@RequestBody UserDTO userDTO)
    {
        return userService.register(userDTO);
    }

    @Operation(summary = "Login user")
    @PostMapping("/login")
    public Response<Void> login(@RequestParam String username,@RequestParam String password)
    {
        return userService.login(username,password);
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/get/{id}")
    public Response<UserVO> findById(@PathVariable Long id)
    {
        return userService.findById(id);
    }

    @Operation(summary = "Update user")
    @PostMapping("/update")
    public Response<Void> updateUser(@RequestBody UserDTO userDTO)
    {
        return userService.updateUser(userDTO);
    }

    @Operation(summary = "update password")
    @PostMapping("/update/")
    public Response<Void> updatePassword(@RequestParam String username,@RequestParam String newPassword)
    {
        return userService.updatePassword(username,newPassword);
    }

}
