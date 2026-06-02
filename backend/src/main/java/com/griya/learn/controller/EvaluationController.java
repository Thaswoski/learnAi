package com.griya.learn.controller;

import com.griya.learn.service.EvaluationService;
import com.griya.learn.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final UserService userService;

    public EvaluationController(EvaluationService evaluationService, UserService userService) {
        this.evaluationService = evaluationService;
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Object> getEvaluation(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        return evaluationService.getEvaluation(userId);
    }
}
