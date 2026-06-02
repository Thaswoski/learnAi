package com.griya.learn.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.StudentProfile;
import com.griya.learn.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class PathPlannerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PathPlannerAgent.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    private static final String PATH_PLANNER_PROMPT =
        "你是一个专业的学习路径规划师(PathPlannerAgent)，属于多智能体系统中的路径规划角色。" +
        "你的任务是根据学生的画像数据和学习情况，制定个性化学习路径。\n\n" +
        "严格按以下JSON格式输出，不要输出任何其他内容：\n" +
        "{\n" +
        "  \"steps\": [\n" +
        "    {\"title\":\"阶段名\",\"status\":\"已完成|进行中|未开始\",\"duration\":小时数,\"tags\":[\"知识点1\",\"知识点2\"]}\n" +
        "  ],\n" +
        "  \"weeklyPlan\": [\n" +
        "    {\"day\":\"周一\",\"task\":\"具体任务描述\",\"duration\":\"X小时\",\"status\":\"未开始\"}\n" +
        "  ],\n" +
        "  \"recommendedResources\": [\n" +
        "    {\"type\":\"mindmap|ppt|exercise|reading|lecture\",\"title\":\"资源标题\",\"reason\":\"推荐理由\"}\n" +
        "  ]\n" +
        "}\n\n" +
        "要求：\n" +
        "1. steps按学习路径从基础到进阶排列，3-5个阶段\n" +
        "2. 根据数据中标记为已完成/进行中的知识点更新stages状态\n" +
        "3. weeklyPlan必须是7天（周一到周日），每天1-2个任务，总时长≤3小时\n" +
        "4. recommendedResources根据学生薄弱点推荐合适的资源类型\n" +
        "5. 如果提供了画像数据，要结合学习节奏和认知偏好制定计划";

    public PathPlannerAgent(@Qualifier("deepseekAiService") AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "PathPlannerAgent";
    }

    @Override
    public String getRole() {
        return "路径规划师";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        try {
            StringBuilder userData = new StringBuilder();

            // 从上下文获取答题数据
            List<Map<String, Object>> knowledgeMastery = context.get("knowledgeMastery");
            if (knowledgeMastery != null) {
                userData.append("知识点掌握情况:\n");
                for (Map<String, Object> km : knowledgeMastery) {
                    userData.append("- ").append(km.get("name"))
                        .append(": ").append(km.get("mastery")).append("%\n");
                }
                userData.append("\n");
            }

            // 从上下文获取画像数据
            StudentProfile profile = context.getProfile();
            if (profile != null) {
                userData.append("学生画像:\n");
                if (profile.getOverallLevel() != null) {
                    userData.append("- 整体水平: ").append(profile.getOverallLevel()).append("\n");
                }
                if (profile.getStudyRhythm() != null) {
                    try {
                        Map<String, Object> rhythm = objectMapper.readValue(
                            profile.getStudyRhythm(), new TypeReference<Map<String, Object>>() {});
                        userData.append("- 学习节奏: ").append(rhythm.getOrDefault("studySlot", "未知")).append("\n");
                        userData.append("- 专注时长: ").append(rhythm.getOrDefault("focusDuration", "未知")).append("\n");
                    } catch (Exception ignored) {}
                }
                if (profile.getCognitiveStyle() != null) {
                    try {
                        Map<String, Object> cognitive = objectMapper.readValue(
                            profile.getCognitiveStyle(), new TypeReference<Map<String, Object>>() {});
                        userData.append("- 媒体偏好: ").append(cognitive.getOrDefault("mediaPreference", "未知")).append("\n");
                    } catch (Exception ignored) {}
                }
                if (profile.getLearningGoal() != null) {
                    try {
                        Map<String, Object> goal = objectMapper.readValue(
                            profile.getLearningGoal(), new TypeReference<Map<String, Object>>() {});
                        userData.append("- 学习目标: ").append(goal.getOrDefault("purpose", "未知")).append("\n");
                    } catch (Exception ignored) {}
                }
            }

            // 从上下文获取课程信息
            if (context.getCourseName() != null) {
                userData.append("课程: ").append(context.getCourseName()).append("\n");
            }

            log.info("[PathPlannerAgent] 开始规划路径: userId={}", context.getUserId());

            if (onStep != null) onStep.accept("正在根据画像制定学习路径...");

            String response = aiService.chat(PATH_PLANNER_PROMPT, userData.toString(), null);

            String json = extractJson(response);
            Map<String, Object> parsed = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> steps = (List<Map<String, Object>>) parsed.get("steps");
            List<Map<String, Object>> weeklyPlan = (List<Map<String, Object>>) parsed.get("weeklyPlan");
            List<Map<String, Object>> recommendedResources = (List<Map<String, Object>>) parsed.get("recommendedResources");

            // 注入CSS类名
            if (steps != null) {
                for (Map<String, Object> step : steps) {
                    String status = (String) step.getOrDefault("status", "未开始");
                    step.put("statusClass", statusToClass(status));
                }
            }
            if (weeklyPlan != null) {
                for (Map<String, Object> plan : weeklyPlan) {
                    String status = (String) plan.getOrDefault("status", "未开始");
                    plan.put("statusClass", statusToClass(status));
                }
            }

            // 存入上下文
            context.put("learningPath", Map.of(
                "steps", steps != null ? steps : List.of(),
                "weeklyPlan", weeklyPlan != null ? weeklyPlan : List.of(),
                "recommendedResources", recommendedResources != null ? recommendedResources : List.of()
            ));

            String summary = "已规划学习路径，共" + (steps != null ? steps.size() : 0)
                + "个阶段、" + (weeklyPlan != null ? weeklyPlan.size() : 0) + "天计划";

            log.info("[PathPlannerAgent] 路径规划完成: {}", summary);

            return AgentResult.ok(summary, Map.of(
                "steps", steps != null ? steps : List.of(),
                "weeklyPlan", weeklyPlan != null ? weeklyPlan : List.of(),
                "recommendedResources", recommendedResources != null ? recommendedResources : List.of()
            ));

        } catch (Exception e) {
            log.error("[PathPlannerAgent] 路径规划失败", e);
            return AgentResult.fail("路径规划失败: " + e.getMessage());
        }
    }

    private String statusToClass(String status) {
        if ("已完成".equals(status)) return "badge-green";
        if ("进行中".equals(status)) return "badge-orange";
        return "badge-blue";
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return sanitizeJson(text.substring(start, end + 1));
        }
        return "{}";
    }

    private String sanitizeJson(String json) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"' || next == '\\' || next == '/' || next == 'b'
                    || next == 'f' || next == 'n' || next == 'r' || next == 't' || next == 'u') {
                    sb.append(c);
                } else {
                    sb.append("\\\\");
                }
            } else if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
