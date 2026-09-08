package com.schoolMarket.mapper;

import com.schoolMarket.dto.UserRegisterDTO;
import com.schoolMarket.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from User where username=#{username}")
    User findByUsername(@Param("username") String username);

    @Insert("insert into User(username,password,phone,nickname) values(#{username},#{password},#{phone}),#{nickname}")
    void insert(UserRegisterDTO userRegisterDTO);
}
