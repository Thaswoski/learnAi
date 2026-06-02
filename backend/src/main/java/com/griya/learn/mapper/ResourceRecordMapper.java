package com.griya.learn.mapper;

import com.griya.learn.entity.ResourceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResourceRecordMapper {
    int insert(ResourceRecord record);
    List<ResourceRecord> selectByUser(@Param("userId") Long userId);
    int countByUser(@Param("userId") Long userId);
}
