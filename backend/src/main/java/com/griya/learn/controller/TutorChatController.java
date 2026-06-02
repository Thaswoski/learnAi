package com.griya.learn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.config.WebResourceConfig;
import com.griya.learn.entity.ChatMessage;
import com.griya.learn.mapper.ChatMessageMapper;
import com.griya.learn.service.AiService;
import com.griya.learn.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/tutor")
public class TutorChatController {

    private static final Logger log = LoggerFactory.getLogger(TutorChatController.class);

    private final UserService userService;
    private final AiService qwenAiService;
    private final AiService sparkAiService;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String activeModel = "qwen";

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public TutorChatController(UserService userService,
                                @Qualifier("qwenAiService") AiService qwenAiService,
                                @Qualifier("sparkAiService") AiService sparkAiService,
                                ChatMessageMapper chatMessageMapper) {
        this.userService = userService;
        this.qwenAiService = qwenAiService;
        this.sparkAiService = sparkAiService;
        this.chatMessageMapper = chatMessageMapper;
    }

    private AiService getActiveService() {
        return "spark".equals(activeModel) ? sparkAiService : qwenAiService;
    }

    @GetMapping("/model")
    public Map<String, Object> getModel() {
        AiService svc = getActiveService();
        return Map.of("code", 200, "data", Map.of(
            "active", activeModel,
            "name", svc.getModelName(),
            "supportsImage", svc.supportsImage()
        ));
    }

    @PostMapping("/model")
    public Map<String, Object> setModel(@RequestBody Map<String, String> body) {
        String model = body.getOrDefault("model", "qwen");
        if (!"qwen".equals(model) && !"spark".equals(model)) {
            return Map.of("code", 400, "message", "无效模型: " + model + "，可选: qwen, spark");
        }
        activeModel = model;
        AiService svc = getActiveService();
        return Map.of("code", 200, "data", Map.of(
            "active", activeModel,
            "name", svc.getModelName(),
            "supportsImage", svc.supportsImage()
        ));
    }

    @PostMapping("/chat")
    public void chat(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body,
            HttpServletResponse response) throws Exception {

        Long userId = userService.getUserByToken(token).getId();
        String prompt = (String) body.getOrDefault("prompt", "");
        String base64Image = (String) body.getOrDefault("image", null);
        String sessionId = (String) body.getOrDefault("sessionId", String.valueOf(System.currentTimeMillis()));

        if ((prompt == null || prompt.isBlank()) && (base64Image == null || base64Image.isEmpty())) {
            response.setContentType("text/event-stream; charset=UTF-8");
            OutputStream out = response.getOutputStream();
            String line = "data: {\"type\":\"error\",\"content\":\"消息不能为空\"}\n\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
            return;
        }

        AiService aiService = getActiveService();

        String imagePath = null;
        if (base64Image != null && !base64Image.isEmpty()) {
            imagePath = saveBase64Image(userId, sessionId, base64Image);
        }

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

        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(prompt);
        userMsg.setImageUrl(imagePath);
        chatMessageMapper.insert(userMsg);

        response.setContentType("text/event-stream; charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        OutputStream out = response.getOutputStream();

        try {
            String fullResponse = aiService.tutorChat(prompt, base64Image, history, chunk -> {
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

            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setUserId(userId);
            aiMsg.setSessionId(sessionId);
            aiMsg.setRole("assistant");
            aiMsg.setContent(fullResponse);
            chatMessageMapper.insert(aiMsg);

            Map<String, Object> done = new HashMap<>();
            done.put("type", "done");
            done.put("fullContent", fullResponse);
            done.put("sessionId", sessionId);
            String line = "data: " + objectMapper.writeValueAsString(done) + "\n\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (Exception e) {
            log.error("AI tutor chat error", e);
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

    private String saveBase64Image(Long userId, String sessionId, String base64) {
        try {
            File dir = new File(WebResourceConfig.resolveDir(uploadDir), "tutor_images");
            if (!dir.exists()) dir.mkdirs();
            String fileName = "tutor_" + userId + "_" + sessionId + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            File dest = new File(dir, fileName);
            byte[] bytes = Base64.getDecoder().decode(base64);
            Files.write(dest.toPath(), bytes);
            String urlPath = "/uploads/tutor_images/" + fileName;
            log.info("辅导图片已保存: {}", urlPath);
            return urlPath;
        } catch (Exception e) {
            log.error("保存辅导图片失败", e);
            return null;
        }
    }

    @GetMapping("/sessions")
    public Map<String, Object> getSessions(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        List<String> sessions = chatMessageMapper.selectSessions(userId);
        return Map.of("code", 200, "data", sessions);
    }

    @GetMapping("/messages/{sessionId}")
    public Map<String, Object> getMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        Long userId = userService.getUserByToken(token).getId();
        List<ChatMessage> messages = chatMessageMapper.selectBySession(userId, sessionId);
        return Map.of("code", 200, "data", messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> deleteSession(
            @RequestHeader("Authorization") String token,
            @PathVariable String sessionId) {
        Long userId = userService.getUserByToken(token).getId();
        chatMessageMapper.deleteBySession(userId, sessionId);
        return Map.of("code", 200, "message", "删除成功");
    }
}
