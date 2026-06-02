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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class CollaborativeOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CollaborativeOrchestrator.class);

    private final PlannerAgent plannerAgent;
    private final SearchAgent searchAgent;
    private final ContentAgent contentAgent;
    private final ReviewerAgent reviewerAgent;
    private final PathPlannerAgent pathPlannerAgent;
    private final PPTAgent pptAgent;
    private final StudentProfileService profileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PYTHON_SERVICE_URL = "http://localhost:5050";

    private static final int MAX_REFINE_ROUNDS = 3;
    private static final int MIN_SCORE_THRESHOLD = 70;
    private static final int MIN_SEARCH_RESULTS = 3;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public CollaborativeOrchestrator(PlannerAgent plannerAgent,
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
                loadProfile(context);

                Consumer<String> onProgress = msg -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(msg));
                    } catch (Exception ignored) {}
                };

                // ─── 创建事件总线并注入上下文 ───
                AgentEventBus eventBus = new AgentEventBus();
                context.setEventBus(eventBus);
                AtomicBoolean planAdjusted = new AtomicBoolean(false);

                // ─── 注册Agent间协同事件 ───

                // 搜索不足 → 切换到宽泛搜索策略
                eventBus.on(AgentEvent.Type.SEARCH_INSUFFICIENT, ev -> {
                    log.warn("[协同] 搜索不足事件: 结果数={}, 策略切换", String.valueOf(ev.get("count")));
                    onProgress.accept("搜索资源不足,切换为宽泛搜索策略...");
                    rawParams.put("searchModel", "deepseek");
                });

                // 搜索结果与计划冲突 → 回调规划师调整
                eventBus.on(AgentEvent.Type.PLAN_ADJUST_NEEDED, ev -> {
                    log.info("[协同] 规划师收到调整请求: reason={}", String.valueOf(ev.get("reason")));
                    onProgress.accept("检测到搜索资源与教学计划存在差异,规划师正在调整策略...");
                    planAdjusted.set(true);
                });

                // ─── 主工作流: 带迭代优化循环 ───
                WorkflowEngine engine = new WorkflowEngine().onProgress(onProgress);

                // Step 1: 课程规划
                engine.addStep(TaskStep.of("plan",
                    "课程规划师", "正在分析学习需求,制定教学计划...",
                    "ri-compass-3-line",
                    () -> {
                        AgentResult result = plannerAgent.execute(context, onProgress);
                        context.emit(AgentEvent.Type.CONTENT_GENERATED, "PlannerAgent",
                            Map.of("topic", context.get("topic", "")));
                        return result;
                    }));

                // Step 2: 讯飞搜索 (需要时)
                String resType = context.getResourceType();
                boolean needSearch = "ppt".equals(resType) || "lecture".equals(resType)
                    || "exercise".equals(resType) || "reading".equals(resType);
                if (needSearch) {
                    engine.addStep(TaskStep.of("search",
                        "讯飞搜索员", "正在通过讯飞ONE SEARCH全网搜索相关教学资源...",
                        "ri-search-line",
                        () -> {
                            AgentResult result = searchAgent.execute(context, onProgress);
                            if (result.isSuccess()) {
                                String sr = context.get("searchResults");
                                if (sr != null && !sr.isEmpty()) {
                                    rawParams.put("searchResults", sr);
                                    Object countObj = context.get("searchItemCount");
                                    int count = countObj != null ? Integer.parseInt(String.valueOf(countObj)) : 0;
                                    rawParams.put("searchItemCount", String.valueOf(count));

                                    // ── 发布协同事件 ──
                                    if (count < MIN_SEARCH_RESULTS) {
                                        context.emit(AgentEvent.Type.SEARCH_INSUFFICIENT, "SearchAgent",
                                            Map.of("count", count, "query", context.getCourseName()));
                                        onProgress.accept("搜索资源偏少(" + count + "条),将补充通用搜索...");
                                    }
                                }
                            }
                            return result;
                        }));
                }

                // Step 3: Python 生成 或 Java ContentAgent 生成
                AtomicInteger refineRound = new AtomicInteger(0);
                AtomicReference<Map<String, Object>> lastPythonResult = new AtomicReference<>();

                engine.addStep(TaskStep.of("content_gen",
                    "AI生成引擎", "正在调用AI服务生成多模态资源...",
                    "ri-robot-2-line",
                    () -> {
                        AgentResult genResult;
                        boolean usePython = needSearch || "ppt".equals(resType);
                        if (usePython) {
                            genResult = callPythonService(context, rawParams, emitter);
                            if (genResult.isSuccess() && context.has("pythonResult")) {
                                lastPythonResult.set(context.get("pythonResult"));
                            }
                        } else {
                            genResult = contentAgent.execute(context, onProgress);
                        }
                        context.emit(AgentEvent.Type.CONTENT_GENERATED, "ContentEngine",
                            Map.of("success", genResult.isSuccess()));
                        return genResult;
                    }));

                // Step 4: 质量审核 + 迭代优化循环
                engine.addStep(TaskStep.of("review_refine",
                    "质量审核与迭代优化", "正在进行内容质量审核...",
                    "ri-shield-check-line",
                    () -> executeReviewRefineLoop(context, rawParams, emitter, onProgress, planAdjusted, refineRound, lastPythonResult)));

                engine.execute();

                // Step 5: 如果有最终Python结果,做最终审核
                Map<String, Object> finalPythonResult = context.get("pythonResult");
                if (finalPythonResult == null && lastPythonResult.get() != null) {
                    finalPythonResult = lastPythonResult.get();
                }

                AgentResult finalReview = reviewerAgent.execute(context, onProgress);
                Map<String, Object> review = context.get("review");

                // ── 发送完成事件 ──
                Map<String, Object> doneEvent = new LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("success", engine.isAllSuccess());
                doneEvent.put("title", context.get("title"));
                doneEvent.put("content", context.get("content"));
                doneEvent.put("review", review);
                doneEvent.put("refineRounds", refineRound.get());
                doneEvent.put("planAdjusted", planAdjusted.get());

                if (finalPythonResult != null) {
                    doneEvent.put("filename", finalPythonResult.get("filename"));
                    doneEvent.put("mimeType", finalPythonResult.get("mimeType"));
                    doneEvent.put("downloadUrl", finalPythonResult.get("downloadUrl"));
                    doneEvent.put("base64", finalPythonResult.get("base64"));
                }

                TaskStep lastFailed = engine.getLastFailed();
                if (lastFailed != null && lastFailed.getResult() != null) {
                    doneEvent.put("error", lastFailed.getResult().getErrorMessage());
                }

                doneEvent.put("collaborationLog", eventBus.history().stream()
                    .map(e -> Map.of(
                        "type", e.type().name(),
                        "source", e.sourceAgent(),
                        "payload", e.payload()
                    ))
                    .toList());

                context.emit(AgentEvent.Type.WORKFLOW_DONE, "CollaborativeOrchestrator",
                    Map.of("rounds", refineRound.get(), "adjusted", planAdjusted.get()));

                emitter.send(SseEmitter.event().name("done").data(doneEvent));
                emitter.complete();

            } catch (Exception e) {
                log.error("[协同编排] 工作流异常", e);
                try {
                    if (context.getEventBus() != null) {
                        context.emit(AgentEvent.Type.WORKFLOW_ERROR, "CollaborativeOrchestrator",
                            Map.of("error", e.getMessage()));
                    }
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "系统异常: " + e.getMessage())));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    /**
     * 审核→修正→再审 迭代循环
     *
     * 核心协同逻辑:
     * 1. ReviewerAgent 审核内容,输出评分和问题列表
     * 2. 如果评分 < 70分,提取问题反馈给Python服务重新生成
     * 3. 最多迭代 MAX_REFINE_ROUNDS 轮
     * 4. 每一轮都将上一轮的反馈作为context传递给内容生成器
     */
    private AgentResult executeReviewRefineLoop(
        AgentContext context,
        Map<String, String> rawParams,
        SseEmitter emitter,
        Consumer<String> onProgress,
        AtomicBoolean planAdjusted,
        AtomicInteger refineRound,
        AtomicReference<Map<String, Object>> lastPythonResult) {

        try {
            for (int round = 0; round < MAX_REFINE_ROUNDS; round++) {
                refineRound.set(round);

                // Step A: 审核
                AgentResult reviewResult = reviewerAgent.execute(context, onProgress);
                Map<String, Object> review = context.get("review");

                if (review == null) {
                    log.warn("[迭代优化] 第{}轮审核无结果,跳过优化", round + 1);
                    return reviewResult;
                }

                int score = ((Number) review.getOrDefault("score", 100)).intValue();
                List<Map<String, Object>> issues = context.get("reviewIssues");
                int issueCount = issues != null ? issues.size() : 0;

                log.info("[迭代优化] 第{}轮审核: score={}, issues={}", round + 1, score, issueCount);

                // 发布审核事件
                if (score >= 85) {
                    context.emit(AgentEvent.Type.REVIEW_PASSED, "ReviewerAgent",
                        Map.of("round", round + 1, "score", score));
                    onProgress.accept("质量审核通过(第" + (round + 1) + "轮,评分" + score + "/100)");
                    return reviewResult;
                }

                if (score < MIN_SCORE_THRESHOLD) {
                    context.emit(AgentEvent.Type.REVIEW_LOW_SCORE, "ReviewerAgent",
                        Map.of("round", round + 1, "score", score, "issues", issueCount));

                    onProgress.accept("质量评分偏低(" + score + "/100),触发第" + (round + 2) + "轮优化...");

                    // Step B: 将反馈合并到rawParams,传递给Python服务
                    StringBuilder feedback = new StringBuilder();
                    feedback.append("【质量改进要求 - 第").append(round + 2).append("轮】\n");
                    feedback.append("上一轮评分: ").append(score).append("/100\n");
                    if (issues != null && !issues.isEmpty()) {
                        feedback.append("需要修复的问题:\n");
                        for (Map<String, Object> issue : issues) {
                            feedback.append("- [").append(issue.getOrDefault("severity", ""))
                                .append("] ").append(issue.getOrDefault("description", ""))
                                .append(" → ").append(issue.getOrDefault("suggestion", "")).append("\n");
                        }
                    }

                    String existingGaps = rawParams.getOrDefault("knowledgeGaps", "");
                    rawParams.put("knowledgeGaps", existingGaps + "\n" + feedback);
                    rawParams.put("refineRound", String.valueOf(round + 1));
                    rawParams.put("refineFeedback", feedback.toString());

                    // Step C: 重新调用生成
                    onProgress.accept("AI生成引擎正在根据反馈优化内容(第" + (round + 2) + "轮)...");
                    AgentResult regenResult = callPythonService(context, rawParams, emitter);
                    if (regenResult.isSuccess() && context.has("pythonResult")) {
                        lastPythonResult.set(context.get("pythonResult"));
                    }
                } else {
                    // 70-84分: 接受但记录
                    context.emit(AgentEvent.Type.REVIEW_PASSED, "ReviewerAgent",
                        Map.of("round", round + 1, "score", score, "accepted", true));
                    onProgress.accept("质量审核通过(第" + (round + 1) + "轮,评分" + score + "/100,接受结果)");
                    return reviewResult;
                }
            }

            // 达到最大迭代轮次,强制接受
            log.warn("[迭代优化] 已达到最大迭代轮次({}),强制接受结果", MAX_REFINE_ROUNDS);
            Map<String, Object> finalReview = context.get("review");
            int finalScore = finalReview != null
                ? ((Number) finalReview.getOrDefault("score", 0)).intValue()
                : 0;
            return AgentResult.ok("迭代优化完成(共" + MAX_REFINE_ROUNDS + "轮,最终评分" + finalScore + "/100)");

        } catch (Exception e) {
            log.error("[迭代优化] 异常", e);
            return AgentResult.ok("迭代优化异常跳过,内容已生成");
        }
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

            log.info("[协同编排] 调用Python服务");

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("[协同编排] Python服务返回 {}: {}", response.statusCode(), errBody);
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

                        Map<String, Object> fwdEvent = new LinkedHashMap<>();
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
                Map<String, Object> pythonResult = new LinkedHashMap<>();
                pythonResult.put("filename", doneData.getOrDefault("filename", ""));
                pythonResult.put("mimeType", doneData.getOrDefault("mimeType", ""));
                pythonResult.put("downloadUrl", doneData.getOrDefault("downloadUrl", ""));
                pythonResult.put("base64", doneData.getOrDefault("base64", ""));
                context.put("pythonResult", pythonResult);

                Map<String, Object> simpleContent = new LinkedHashMap<>();
                simpleContent.put("title", context.get("topic") != null ? context.get("topic") : context.getCourseName());
                simpleContent.put("summary", "由Python多模态生成服务生成的" + context.getResourceType() + "资源");
                simpleContent.put("sections", List.of(Map.of(
                    "heading", "生成结果",
                    "content", "文件已生成: " + doneData.getOrDefault("filename", "")
                )));
                context.put("content", simpleContent);

                String msg = "Python服务生成完成: " + doneData.getOrDefault("filename", "");
                log.info("[协同编排] {}", msg);
                return AgentResult.ok(msg, pythonResult);
            }

            return AgentResult.fail("Python服务未返回完成结果");
        } catch (Exception e) {
            log.error("[协同编排] Python服务调用失败", e);
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

                AgentEventBus eventBus = new AgentEventBus();
                context.setEventBus(eventBus);

                WorkflowEngine engine = new WorkflowEngine().onProgress(onProgress);

                engine.addStep(TaskStep.of("path",
                    "路径规划师", "正在结合画像与答题数据制定个性化学习路径...",
                    "ri-route-line",
                    () -> {
                        AgentResult result = pathPlannerAgent.execute(context, onProgress);
                        context.emit(AgentEvent.Type.WORKFLOW_DONE, "PathPlannerAgent");
                        return result;
                    }));

                engine.execute();

                Map<String, Object> pathResult = context.get("learningPath");
                if (pathResult == null) {
                    pathResult = Map.of();
                }

                Map<String, Object> doneEvent = new LinkedHashMap<>();
                doneEvent.put("type", "done");
                doneEvent.put("success", engine.isAllSuccess());
                doneEvent.put("steps", pathResult.getOrDefault("steps", List.of()));
                doneEvent.put("weeklyPlan", pathResult.getOrDefault("weeklyPlan", List.of()));
                doneEvent.put("recommendedResources", pathResult.getOrDefault("recommendedResources", List.of()));

                emitter.send(SseEmitter.event().name("done").data(doneEvent));
                emitter.complete();
            } catch (Exception e) {
                log.error("[协同编排] 路径规划异常", e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", "规划失败: " + e.getMessage())));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private void loadProfile(AgentContext context) {
        if (context.getUserId() != null) {
            StudentProfile profile = profileService.getByUserId(context.getUserId());
            if (profile != null) {
                context.setProfile(profile);
            }
        }
    }
}
