package com.griya.learn.community.mapper;

import com.griya.learn.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    Comment selectById(@Param("id") Long id);
    List<Comment> selectByPostId(@Param("postId") Long postId);
    int insert(Comment comment);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
