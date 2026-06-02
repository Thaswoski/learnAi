package com.griya.learn.mapper;

import com.griya.learn.entity.DashboardCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DashboardCacheMapper {

    DashboardCache selectByUser(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId,
               @Param("quizTotal") int quizTotal,
               @Param("resourceTotal") int resourceTotal,
               @Param("dataJson") String dataJson);
}
