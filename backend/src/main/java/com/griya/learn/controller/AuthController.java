package com.griya.learn.controller;

import com.griya.learn.common.Result;
import com.griya.learn.dto.LoginRequest;
import com.griya.learn.dto.LoginResponse;
import com.griya.learn.dto.RegisterRequest;
import com.griya.learn.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        LoginResponse resp = userService.register(req);
        return Result.success("注册成功", resp);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = userService.login(req);
        return Result.success("登录成功", resp);
    }
}
