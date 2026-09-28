package com.schoolMarket.service.impl;

import com.schoolMarket.common.Response;
import com.schoolMarket.dto.UserDTO;
import com.schoolMarket.entity.User;
import com.schoolMarket.exception.BusinessException;
import com.schoolMarket.mapper.UserMapper;
import com.schoolMarket.service.UserService;
import com.schoolMarket.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    public Response<Void> register(UserDTO userRegisterDTO) {
        User user=userMapper.findByUsername(userRegisterDTO.getUsername());
        if (user!=null)
        {
            throw new BusinessException("Username exists.");
        }

        userMapper.insert(userRegisterDTO);
        return Response.success(null,"Successfully register user: {" + userRegisterDTO + "}");
    }

    public Response<Void> login(String username,String password)
    {
        User user=userMapper.findByUsername(username);
        if (user==null)
        {
            throw new BusinessException("User is not exist.");
        }
        /*
          safety component
         */
        return Response.success(null,"Successfully login user:" + user);
    }

    public Response<UserVO> findById(Long id)
    {
        User user=userMapper.findById(id);
        if (user==null)
        {
            throw new BusinessException("User does not exist: " + id);
        }
        return Response.success(user.ToVO(user),"Successfully find: " + user);
    }

    public Response<Void> updateUser(UserDTO userDTO)
    {
        User user=userMapper.findByUsername(userDTO.getUsername());
        if (user==null)
        {
            throw new BusinessException("User does not exist: " + userDTO);
        }
        userMapper.update(userDTO);
        return Response.success(null,"Successfully update user: " + userDTO);
    }

    public Response<Void> updatePassword(String username, String newPassword)
    {
        User user=userMapper.findByUsername(username);
        if (user==null)
        {
            throw new BusinessException("User does not exist: " + username);
        }
        userMapper.updatePassword(username,newPassword);
        return Response.success(null,"Successfully update password: " + username);
    }


}
