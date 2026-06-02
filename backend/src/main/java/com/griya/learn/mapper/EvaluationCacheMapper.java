package com.griya.learn.mapper;

import com.griya.learn.entity.EvaluationCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationCacheMapper {

    EvaluationCache selectByUser(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId,
               @Param("quizTotal") int quizTotal,
               @Param("dataJson") String dataJson);
}
