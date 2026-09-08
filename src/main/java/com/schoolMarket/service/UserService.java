package com.schoolMarket.service;

import com.schoolMarket.common.Response;
import com.schoolMarket.dto.UserRegisterDTO;

public interface UserService {

    Response<Void> register(UserRegisterDTO userRegisterDTO);




}
