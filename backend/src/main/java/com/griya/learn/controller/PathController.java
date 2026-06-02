package com.griya.learn.controller;

import com.griya.learn.service.LearningPathService;
import com.griya.learn.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/path")
public class PathController {

    private final LearningPathService learningPathService;
    private final UserService userService;

    public PathController(LearningPathService learningPathService, UserService userService) {
        this.learningPathService = learningPathService;
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Object> getPath(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        return learningPathService.generatePath(userId);
    }
}
