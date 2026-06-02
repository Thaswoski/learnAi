package com.griya.learn.community.mapper;

import com.griya.learn.community.entity.PostAttitude;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostAttitudeMapper {
    PostAttitude selectByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);
    int insert(PostAttitude attitude);
    int updateById(PostAttitude attitude);
    int deleteById(@Param("id") Long id);
}
