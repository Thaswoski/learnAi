package com.griya.learn.mapper;

import com.griya.learn.entity.CQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuizMapper {

    List<CQuestion> selectAll();

    CQuestion selectById(@Param("id") Integer id);

    List<CQuestion> selectByKnowledgePoint(@Param("knowledgePoint") String knowledgePoint);

    List<CQuestion> selectByDifficulty(@Param("difficulty") String difficulty);

    List<CQuestion> selectByKeyword(@Param("keyword") String keyword);

    List<CQuestion> selectByCondition(@Param("difficulty") String difficulty,
                                       @Param("knowledgePoint") String knowledgePoint,
                                       @Param("keyword") String keyword);

    List<String> selectAllKnowledgePoints();

    List<CQuestion> selectRandom(@Param("difficulty") String difficulty,
                                  @Param("limit") int limit);

    int countTotal();

    int countByDifficulty(@Param("difficulty") String difficulty);
}
