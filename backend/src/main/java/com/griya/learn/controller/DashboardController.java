package com.griya.learn.controller;

import com.griya.learn.service.DashboardService;
import com.griya.learn.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping
    public Map<String, Object> getDashboard(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        return dashboardService.getDashboard(userId);
    }
}
