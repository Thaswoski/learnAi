package com.griya.learn.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.StudentProfile;
import com.griya.learn.service.StudentProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class MultiAgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(MultiAgentOrchestrator.class);

    private final PlannerAgent plannerAgent;
    private final SearchAgent searchAgent;
    private final ContentAgent contentAgent;
    private final ReviewerAgent reviewerAgent;
    private final PathPlannerAgent pathPlannerAgent;
    private final PPTAgent pptAgent;
    private final StudentProfileService profileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PYTHON_SERVICE_URL = "http://localhost:5050";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public MultiAgentOrchestrator(PlannerAgent plannerAgent,
                                   SearchAgent searchAgent,
                                   ContentAgent contentAgent,
                                   ReviewerAgent reviewerAgent,
                                   PathPlannerAgent pathPlannerAgent,
                                   PPTAgent pptAgent,
                                   StudentProfileService profileService) {
        this.plannerAgent = plannerAgent;
        this.searchAgent = searchAgent;
        this.contentAgent = contentAgent;
        this.reviewerAgent = reviewerAgent;
        this.pathPlannerAgent = pathPlannerAgent;
        this.pptAgent = pptAgent;
        this.profileService = profileService;
    }

    public SseEmitter generateResourceStream(AgentContext context, Map<String, String> rawParams) {
        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                if (context.getUserId() != null) {
                    StudentProfile profile = profileService.getByUserId(context.getUserId());
                    if (profile != null) {
                        context.setProfile(profile);
                    }
                }

                Consumer<String> onProgress = msg -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(msg));
                    } catch (Exception e) {
                        log.debug("SSE推送失败: {}", e.getMessage());
                    }
                };

                // ─── 混合编排工作流 ───
                WorkflowEngine engine = new WorkflowEngine().onProgress(onProgress);

                // Step 1: 课程规划师
                engine.addStep(TaskStep.of("plan",
                    "课程规划师", "正在分析学习需求，制定教学计划...",
                    "ri-compass-3-line",
                    () -> plannerAgent.execute(context, onProgress)));

                // Step 2: 讯飞搜索（ppt/lecture/exercise/reading 类型均需搜索）
                String resType = context.getResourceType();
                boolean needSearch = "ppt".equals(resType) || "lecture".equals(resType)
                    || "exercise".equals(resType) || "reading".equals(resType);
                if (needSearch) {
                    engine.addStep(TaskStep.of("search",
                        "讯飞搜索员", "正在通过讯飞 ONE SEARCH 全网搜索相关教学资源...",
                        "ri-search-line",
                        () -> {
                            AgentResult searchResult = searchAgent.execute(context, onProgress);
                            if (searchResult.isSuccess()) {
                                String sr = context.get("searchResults");
                                if (sr != null && !sr.isEmpty()) {
                                    rawParams.put("searchResults", sr);
                                    Object countObj = context.get("searchItemCount");
                                    rawParams.put("searchItemCount", countObj != null ? String.valueOf(countObj) : "0");
                                }
                            }
                            return searchResult;
                        }));
                }

                // Step 3: Python 服务生成文件
                engine.addStep(TaskStep.of("python_gen",
                    "AI生成引擎", "正在调用Python服务生成多模态资源...",
                    "ri-robot-2-line",
                    () -> callPythonService(context, rawParams, emitter)));

                // Step 4: 质量审核
                engine.addStep(TaskStep.of("review",
                    "质量审核员", "正在进行内容质量审核...",
                    "ri-shield-check-line",
                    () -> reviewerAgent.execute(context, onProgress)));

                engine.execute();

                // 发送完成事件
                Map<String, Object> doneEvent = new java.util.LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("success", engine.isAllSuccess());
                doneEvent.put("title", context.get("title"));
                doneEvent.put("content", context.get("content"));
                doneEvent.put("review", context.get("review"));

                Map<String, Object> pythonResult = context.get("pythonResult");
                if (pythonResult != null) {
                    doneEvent.put("filename", pythonResult.get("filename"));
                    doneEvent.put("mimeType", pythonResult.get("mimeType"));
                    doneEvent.put("downloadUrl", pythonResult.get("downloadUrl"));
                    doneEvent.put("base64", pythonResult.get("base64"));
                }

                TaskStep lastFailed = engine.getLastFailed();
                if (lastFailed != null && lastFailed.getResult() != null) {
                    doneEvent.put("error", lastFailed.getResult().getErrorMessage());
                }

                emitter.send(SseEmitter.event().name("done").data(doneEvent));
                emitter.complete();

            } catch (Exception e) {
                log.error("[Orchestrator] 工作流异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "系统异常: " + e.getMessage())));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private AgentResult callPythonService(AgentContext context, Map<String, String> rawParams, SseEmitter emitter) {
        try {
            String json = objectMapper.writeValueAsString(rawParams);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_SERVICE_URL + "/api/generate/stream"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofMinutes(5))
                .build();

            log.info("[Orchestrator] 调用Python服务: {}", PYTHON_SERVICE_URL);

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("[Orchestrator] Python服务返回 {}: {}", response.statusCode(), errBody);
                return AgentResult.fail("Python生成服务异常(HTTP " + response.statusCode() + ")");
            }

            Map<String, Object> doneData = null;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;

                    try {
                        Map<String, Object> evt = objectMapper.readValue(data,
                            new TypeReference<Map<String, Object>>() {});

                        String step = (String) evt.get("step");

                        // 转发 Python 进度到前端
                        Map<String, Object> fwdEvent = new java.util.LinkedHashMap<>();
                        fwdEvent.put("type", "python_step");
                        fwdEvent.put("step", step);
                        fwdEvent.put("message", evt.getOrDefault("message", ""));
                        fwdEvent.put("icon", evt.getOrDefault("icon", "ri-loader-4-line"));
                        emitter.send(SseEmitter.event().name("progress").data(fwdEvent));

                        if ("done".equals(step)) {
                            doneData = evt;
                        }
                    } catch (Exception e) {
                        log.debug("Python SSE解析跳过: {}", e.getMessage());
                    }
                }
            }

            if (doneData != null) {
                Map<String, Object> pythonResult = new java.util.LinkedHashMap<>();
                pythonResult.put("filename", doneData.getOrDefault("filename", ""));
                pythonResult.put("mimeType", doneData.getOrDefault("mimeType", ""));
                pythonResult.put("downloadUrl", doneData.getOrDefault("downloadUrl", ""));
                pythonResult.put("base64", doneData.getOrDefault("base64", ""));
                context.put("pythonResult", pythonResult);

                // 为 ReviewerAgent 准备简化的内容
                Map<String, Object> simpleContent = new java.util.LinkedHashMap<>();
                simpleContent.put("title", context.get("topic") != null ? context.get("topic") : context.getCourseName());
                simpleContent.put("summary", "由Python多模态生成服务生成的" + context.getResourceType() + "资源");
                simpleContent.put("sections", java.util.List.of(java.util.Map.of(
                    "heading", "生成结果",
                    "content", "文件已生成: " + doneData.getOrDefault("filename", "")
                )));
                context.put("content", simpleContent);

                String msg = "Python服务生成完成: " + doneData.getOrDefault("filename", "");
                log.info("[Orchestrator] {}", msg);
                return AgentResult.ok(msg, pythonResult);
            }

            return AgentResult.fail("Python服务未返回完成结果");
        } catch (Exception e) {
            log.error("[Orchestrator] Python服务调用失败", e);
            return AgentResult.fail("Python生成服务调用失败: " + e.getMessage());
        }
    }

    public SseEmitter generatePathStream(AgentContext context) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Consumer<String> onProgress = msg -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(msg));
                    } catch (Exception ignored) {}
                };

                WorkflowEngine engine = new WorkflowEngine().onProgress(onProgress);

                engine.addStep(TaskStep.of("path",
                    "路径规划师", "正在结合画像与答题数据制定个性化学习路径...",
                    "ri-route-line",
                    () -> pathPlannerAgent.execute(context, onProgress)));

                engine.execute();

                Map<String, Object> pathResult = context.get("learningPath");
                if (pathResult == null) {
                    pathResult = Map.of();
                }

                Map<String, Object> doneEvent = new java.util.LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("success", engine.isAllSuccess());
                doneEvent.put("steps", pathResult.getOrDefault("steps", java.util.List.of()));
                doneEvent.put("weeklyPlan", pathResult.getOrDefault("weeklyPlan", java.util.List.of()));
                doneEvent.put("recommendedResources", pathResult.getOrDefault("recommendedResources", java.util.List.of()));

                emitter.send(SseEmitter.event().name("done").data(doneEvent));
                emitter.complete();
            } catch (Exception e) {
                log.error("[Orchestrator] 路径规划异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "规划失败: " + e.getMessage())));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }
}
