package com.schoolMarket.mapper;

import com.schoolMarket.dto.UserDTO;
import com.schoolMarket.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("select * from User where username=#{username}")
    User findByUsername(@Param("username") String username);

    @Insert("insert into User(username,password,phone,nickname) values(#{username},#{password},#{phone}),#{nickname}")
    void insert(UserDTO userRegisterDTO);

    @Select("select * from User where id=#{id}")
    User findById(@Param("id") Long id);

    @Update("update user set " +
            "phone=#{phone}" +
            "nickname=#{nickname}" +
            "where username=#{username}")
    void update(UserDTO userDTO);

    @Update("update user set password=#{newPassword} where username=#{username}")
    void updatePassword(@Param("username") String username,@Param("newPassword") String newPassword)
}
