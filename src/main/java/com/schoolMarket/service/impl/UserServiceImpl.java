package com.schoolMarket.service.impl;

import com.schoolMarket.dto.UserLoginDTO;
import com.schoolMarket.dto.UserPasswordDTO;
import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.dto.UserUpdateDTO;
import com.schoolMarket.entity.User;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.UserMapper;
import com.schoolMarket.service.UserService;
import com.schoolMarket.util.PasswordEncoder;
import com.schoolMarket.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(UserRegisterDTO dto) {
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("用户名已存在：" + dto.getUsername());
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        // 存哈希，绝不存明文
        user.setPassword(PasswordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname());
        user.setCreateTime(new Date());

        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public UserVO login(UserLoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());

        // 用户不存在与密码错误返回同一句提示，避免泄露「该用户名是否存在」
        if (user == null || !PasswordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setNickname(user.getNickname());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    @Override
    public UserVO getById(Long id) {
        UserVO vo = userMapper.findVOById(id);
        if (vo == null) {
            throw new BusinessException("用户不存在：" + id);
        }
        return vo;
    }

    @Override
    public UserVO getByUsername(String username) {
        UserVO vo = userMapper.findVOByUsername(username);
        if (vo == null) {
            throw new BusinessException("用户不存在：" + username);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserUpdateDTO dto) {
        requireExists(dto.getUsername());
        userMapper.updateProfile(dto.getUsername(), dto.getNickname(), dto.getPhone());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UserPasswordDTO dto) {
        User user = requireExists(dto.getUsername());

        if (!PasswordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (PasswordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        userMapper.updatePassword(dto.getUsername(), PasswordEncoder.encode(dto.getNewPassword()));
    }

    private User requireExists(String username) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在：" + username);
        }
        return user;
    }
}