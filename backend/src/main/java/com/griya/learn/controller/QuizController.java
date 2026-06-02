package com.griya.learn.controller;

import com.griya.learn.entity.CQuestion;
import com.griya.learn.entity.QuizHistory;
import com.griya.learn.service.CodeJudgeService;
import com.griya.learn.service.QuizService;
import com.griya.learn.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;
    private final CodeJudgeService codeJudgeService;
    private final UserService userService;

    public QuizController(QuizService quizService, CodeJudgeService codeJudgeService,
                           UserService userService) {
        this.quizService = quizService;
        this.codeJudgeService = codeJudgeService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "") String difficulty,
            @RequestParam(defaultValue = "") String knowledgePoint,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return quizService.listQuestions(difficulty, knowledgePoint, keyword, page, pageSize);
    }

    @GetMapping("/detail/{id}")
    public CQuestion detail(@PathVariable Integer id) {
        return quizService.getDetail(id);
    }

    @GetMapping("/knowledge-points")
    public List<String> knowledgePoints() {
        return quizService.getAllKnowledgePoints();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return quizService.getStats();
    }

    @PostMapping("/exam")
    public List<CQuestion> generateExam(@RequestBody Map<String, Object> body) {
        String difficulty = (String) body.getOrDefault("difficulty", "");
        int count = body.get("count") instanceof Integer i ? i : 10;
        return quizService.generateExam(difficulty, count);
    }

    @PostMapping("/judge")
    public Map<String, Object> judge(@RequestHeader("Authorization") String token,
                                      @RequestBody Map<String, Object> body) {
        Long userId = userService.getUserByToken(token).getId();
        Integer questionId = body.get("questionId") instanceof Integer i ? i : 0;
        String userCode = (String) body.getOrDefault("code", "");

        CQuestion question = quizService.getDetail(questionId);
        if (question == null) {
            return Map.of("correct", false, "error", "题目不存在");
        }

        Map<String, Object> judgeResult = codeJudgeService.judge(questionId, question.getProblem(),
            userCode, question.getInputExample(), question.getOutputExample());

        QuizHistory history = new QuizHistory();
        history.setUserId(userId);
        history.setQuestionId(questionId);
        history.setQuestionTitle(question.getTitle());
        history.setUserCode(userCode);

        boolean correct = Boolean.TRUE.equals(judgeResult.get("correct"));
        if (judgeResult.containsKey("compileError")) {
            history.setResult("compile_error");
            history.setErrorMessage((String) judgeResult.get("compileError"));
        } else if (judgeResult.containsKey("error")) {
            history.setResult("runtime_error");
            history.setErrorMessage((String) judgeResult.get("error"));
        } else {
            history.setResult(correct ? "correct" : "wrong");
            history.setExpectedOutput((String) judgeResult.get("expected"));
            history.setActualOutput((String) judgeResult.get("actual"));
        }
        quizService.saveHistory(history);

        return judgeResult;
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestHeader("Authorization") String token,
                                        @RequestParam(defaultValue = "50") int limit) {
        Long userId = userService.getUserByToken(token).getId();
        List<QuizHistory> list = quizService.getHistory(userId, limit);
        return Map.of("code", 200, "data", list);
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearHistory(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        int deleted = quizService.clearHistory(userId);
        return Map.of("code", 200, "message", "已清除 " + deleted + " 条记录");
    }
}
