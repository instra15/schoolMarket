package com.schoolMarket.entity;

import com.schoolMarket.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//用户表
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    private Long id;

    private String username;

    private String password;

    private String phone;

    private String nickname;

    public UserVO ToVO(User user)
    {
        UserVO userVO=new UserVO();
        userVO.setNickname(user.getNickname());
        userVO.setPhone(user.getPhone());
        userVO.setUsername(user.getUsername());
        return userVO;
    }
}
