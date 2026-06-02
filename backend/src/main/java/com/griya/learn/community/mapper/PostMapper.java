package com.griya.learn.community.mapper;

import com.griya.learn.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    Post selectById(@Param("id") Long id);
    List<Post> selectPage(@Param("offset") Integer offset, @Param("size") Integer size);
    int count();
    int insert(Post post);
    int updateById(Post post);
    int deleteById(@Param("id") Long id);
    int updateAgreeCount(@Param("id") Long id, @Param("delta") Integer delta);
    int updateDisagreeCount(@Param("id") Long id, @Param("delta") Integer delta);
    int incrementViewCount(@Param("id") Long id);
}
