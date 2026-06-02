package com.griya.learn.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/coze")
public class CozeConfigController {

    @Value("${coze.sat-token}")
    private String satToken;

    @Value("${coze.app-id}")
    private String appId;

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        return Map.of("code", 200, "data", Map.of("satToken", satToken, "appId", appId));
    }
}
