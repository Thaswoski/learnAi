package com.griya.learn.mapper;

import com.griya.learn.entity.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LearningPathMapper {

    LearningPath selectByUser(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId,
               @Param("quizTotal") int quizTotal,
               @Param("dataJson") String dataJson);
}
