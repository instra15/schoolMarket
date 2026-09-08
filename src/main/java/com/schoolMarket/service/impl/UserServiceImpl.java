package com.schoolMarket.service.impl;

import com.schoolMarket.common.Response;
import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.entity.User;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.UserMapper;
import com.schoolMarket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    public Response<Void> register(UserRegisterDTO userRegisterDTO) {
        User user=userMapper.findByUsername(userRegisterDTO.getUsername());
        if (user!=null)
        {
            throw new BusinessException("Username exists.");
        }

        userMapper.insert(userRegisterDTO);
        return Response.success(null,"Successfully register user: {" + userRegisterDTO + "}");
    }
}
