package com.griya.learn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.common.Result;
import com.griya.learn.entity.StudentProfile;
import com.griya.learn.service.DeepSeekService;
import com.griya.learn.service.StudentProfileService;
import com.griya.learn.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class StudentProfileController {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileController.class);

    private final StudentProfileService studentProfileService;
    private final UserService userService;
    private final DeepSeekService deepSeekService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public Result<StudentProfile> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        StudentProfile profile = studentProfileService.getOrCreate(userId);
        return Result.success(profile);
    }

    @PostMapping("/save")
    public Result<StudentProfile> saveProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> data) {
        Long userId = userService.getUserByToken(token).getId();
        StudentProfile profile = studentProfileService.saveProfile(userId, data);
        return Result.success("画像保存成功", profile);
    }

    @PostMapping("/chat")
    public void chat(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body,
            HttpServletResponse response) throws Exception {

        Long userId = userService.getUserByToken(token).getId();
        String prompt = (String) body.getOrDefault("prompt", "");

        List<Map<String, String>> history = new ArrayList<>();
        if (body.get("history") instanceof List) {
            List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("history");
            for (Map<String, Object> m : raw) {
                Map<String, String> msg = new HashMap<>();
                msg.put("role", (String) m.getOrDefault("role", "user"));
                msg.put("content", (String) m.getOrDefault("content", ""));
                history.add(msg);
            }
        }

        response.setContentType("text/event-stream; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();

        try {
            String fullResponse = deepSeekService.chat(prompt, history, chunk -> {
                try {
                    Map<String, String> event = new HashMap<>();
                    event.put("type", "chunk");
                    event.put("content", chunk);
                    String line = "data: " + objectMapper.writeValueAsString(event) + "\n\n";
                    out.write(line.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception e) {
                    log.warn("SSE write chunk failed", e);
                }
            });

            Map<String, String> done = new HashMap<>();
            done.put("type", "done");
            done.put("fullContent", fullResponse);
            String line = "data: " + objectMapper.writeValueAsString(done) + "\n\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            log.error("AI chat error", e);
            Map<String, String> error = new HashMap<>();
            error.put("type", "error");
            error.put("content", e.getMessage());
            String line = "data: " + objectMapper.writeValueAsString(error) + "\n\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } finally {
            out.close();
        }
    }
}
