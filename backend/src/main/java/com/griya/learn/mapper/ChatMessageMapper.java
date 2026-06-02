package com.griya.learn.mapper;

import com.griya.learn.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    List<ChatMessage> selectBySession(@Param("userId") Long userId, @Param("sessionId") String sessionId);
    List<String> selectSessions(@Param("userId") Long userId);
    int insert(ChatMessage message);
    int deleteBySession(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
