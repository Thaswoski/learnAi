package com.griya.learn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.agent.AgentContext;
import com.griya.learn.agent.CollaborativeOrchestrator;
import com.griya.learn.mapper.QuizHistoryMapper;
import com.griya.learn.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final CollaborativeOrchestrator orchestrator;
    private final UserService userService;
    private final QuizHistoryMapper quizHistoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AgentController(CollaborativeOrchestrator orchestrator,
                            UserService userService,
                            QuizHistoryMapper quizHistoryMapper) {
        this.orchestrator = orchestrator;
        this.userService = userService;
        this.quizHistoryMapper = quizHistoryMapper;
    }

    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateResource(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> body) {

        Long userId = userService.getUserByToken(token).getId();

        String courseName = str(body, "courseName");
        String major = str(body, "major");
        String knowledgeGaps = str(body, "knowledgeGaps");
        String learningNeeds = str(body, "learningNeeds");
        String resourceType = str(body, "resourceType");
        if (resourceType == null || resourceType.isEmpty()) resourceType = "lecture";

        AgentContext context = AgentContext.fromUserInput(
            userId, courseName, major, knowledgeGaps, learningNeeds, resourceType);

        // 构建传给Python服务的原始参数
        Map<String, String> pythonParams = new HashMap<>();
        pythonParams.put("model", str(body, "model", "deepseek"));
        pythonParams.put("searchModel", str(body, "searchModel", "xfsearch"));
        pythonParams.put("imageModel", str(body, "imageModel", "seedream"));
        pythonParams.put("major", major != null ? major : "");
        pythonParams.put("courseName", courseName != null ? courseName : "");
        pythonParams.put("knowledgeGaps", knowledgeGaps != null ? knowledgeGaps : "");
        pythonParams.put("learningNeeds", learningNeeds != null ? learningNeeds : "");
        pythonParams.put("docType", resourceType);

        log.info("[AgentController] 启动混合编排: userId={}, course={}, type={}",
            userId, courseName, resourceType);

        return orchestrator.generateResourceStream(context, pythonParams);
    }

    private String str(Map<String, Object> body, String key) {
        return str(body, key, null);
    }

    private String str(Map<String, Object> body, String key, String defaultVal) {
        Object val = body.getOrDefault(key, defaultVal);
        return val != null ? val.toString() : defaultVal;
    }

    @PostMapping(value = "/path", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generatePath(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, Object> body) {

        Long userId = userService.getUserByToken(token).getId();

        AgentContext context = AgentContext.builder()
            .userId(userId)
            .build();

        // 加载答题数据
        List<Map<String, Object>> knowledgeMastery = quizHistoryMapper.knowledgePointMastery(userId);
        context.put("knowledgeMastery", knowledgeMastery);

        if (body != null) {
            if (body.containsKey("courseName")) {
                context.setCourseName((String) body.get("courseName"));
            }
        }

        log.info("[AgentController] 启动路径规划: userId={}", userId);

        return orchestrator.generatePathStream(context);
    }
}
