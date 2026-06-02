package com.griya.learn.mapper;

import com.griya.learn.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectById(@Param("id") Long id);

    User selectByEmail(@Param("email") String email);

    User selectByToken(@Param("token") String token);

    int insert(User user);

    int updateToken(@Param("id") Long id, @Param("token") String token);

    int updateById(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
