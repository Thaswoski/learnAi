package com.griya.learn.mapper;

import com.griya.learn.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentProfileMapper {

    StudentProfile selectByUserId(@Param("userId") Long userId);

    int insert(StudentProfile profile);

    int updateById(StudentProfile profile);
}
