package com.griya.learn.mapper;

import com.griya.learn.entity.QuizHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuizHistoryMapper {

    int insert(QuizHistory record);

    List<QuizHistory> selectByUser(@Param("userId") Long userId, @Param("limit") int limit);

    int deleteAllByUser(@Param("userId") Long userId);

    int correctCount(@Param("userId") Long userId);

    int totalCount(@Param("userId") Long userId);

    List<Map<String, Object>> weeklyStats(@Param("userId") Long userId, @Param("weeks") int weeks);

    List<Map<String, Object>> knowledgePointMastery(@Param("userId") Long userId);

    int todayCount(@Param("userId") Long userId);

    int todayCorrectCount(@Param("userId") Long userId);

    List<Map<String, Object>> recentWeekDailyCount(@Param("userId") Long userId);
}
