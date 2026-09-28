package com.schoolMarket.service;

import com.schoolMarket.common.Response;
import com.schoolMarket.dto.UserDTO;
import com.schoolMarket.vo.UserVO;

public interface UserService {

    Response<Void> register(UserDTO userRegisterDTO);

    Response<Void> login(String username,String password);

    Response<UserVO> findById(Long id);

    Response<Void> updateUser(UserDTO userDTO);

    Response<Void> updatePassword(String username,String newPassword);
}
