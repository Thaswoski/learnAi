package com.griya.learn.service;

import com.griya.learn.entity.CQuestion;
import com.griya.learn.entity.QuizHistory;
import com.griya.learn.mapper.QuizHistoryMapper;
import com.griya.learn.mapper.QuizMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizService {

    private final QuizMapper quizMapper;
    private final QuizHistoryMapper quizHistoryMapper;

    public QuizService(QuizMapper quizMapper, QuizHistoryMapper quizHistoryMapper) {
        this.quizMapper = quizMapper;
        this.quizHistoryMapper = quizHistoryMapper;
    }

    public Map<String, Object> listQuestions(String difficulty, String knowledgePoint,
                                              String keyword, int page, int pageSize) {
        List<CQuestion> all = quizMapper.selectByCondition(difficulty, knowledgePoint, keyword);
        int total = all.size();

        int from = Math.min(page * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<CQuestion> pageData = all.subList(from, to);

        return Map.of(
            "list", pageData,
            "total", total,
            "page", page,
            "pageSize", pageSize
        );
    }

    public CQuestion getDetail(Integer id) {
        return quizMapper.selectById(id);
    }

    public List<String> getAllKnowledgePoints() {
        return quizMapper.selectAllKnowledgePoints();
    }

    public Map<String, Object> getStats() {
        return Map.of(
            "total", quizMapper.countTotal(),
            "easy", quizMapper.countByDifficulty("easy"),
            "medium", quizMapper.countByDifficulty("medium"),
            "hard", quizMapper.countByDifficulty("hard"),
            "knowledgePoints", quizMapper.selectAllKnowledgePoints()
        );
    }

    public List<CQuestion> generateExam(String difficulty, int count) {
        return quizMapper.selectRandom(difficulty, count);
    }

    public void saveHistory(QuizHistory record) {
        quizHistoryMapper.insert(record);
    }

    public List<QuizHistory> getHistory(Long userId, int limit) {
        return quizHistoryMapper.selectByUser(userId, limit);
    }

    public int clearHistory(Long userId) {
        return quizHistoryMapper.deleteAllByUser(userId);
    }
}
