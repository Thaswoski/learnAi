package com.griya.learn.community.mapper;

import com.griya.learn.community.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleMapper {
    Article selectById(@Param("id") Long id);
    List<Article> selectPage(@Param("offset") Integer offset, @Param("size") Integer size, @Param("category") String category);
    int count(@Param("category") String category);
    int insert(Article article);
    int updateById(Article article);
    int incrementViewCount(@Param("id") Long id);
    int incrementLikeCount(@Param("id") Long id);
}
