package com.griya.learn.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class PlannerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    private static final String PLANNER_SYSTEM_PROMPT =
        "你是一个专业的课程规划师(PlannerAgent)，属于多智能体系统中的规划角色。" +
        "你的任务是根据学生信息，制定一份结构化的资源生成计划。\n\n" +
        "严格按以下JSON格式输出计划，不要输出任何其他内容：\n" +
        "{\n" +
        "  \"plan\": {\n" +
        "    \"topic\": \"课程主题\",\n" +
        "    \"targetAudience\": \"目标学生水平描述\",\n" +
        "    \"teachingGoals\": [\"教学目标1\", \"教学目标2\"],\n" +
        "    \"keyPoints\": [\"重点1\", \"重点2\", \"重点3\"],\n" +
        "    \"difficultyPoints\": [\"难点1\", \"难点2\"],\n" +
        "    \"suggestedStructure\": [\"章节1标题\", \"章节2标题\", \"章节3标题\"],\n" +
        "    \"prerequisites\": [\"前置知识1\", \"前置知识2\"],\n" +
        "    \"estimatedDuration\": \"预计学习时长(如'6小时')\",\n" +
        "    \"resourceTypes\": [\"mindmap\", \"ppt\", \"exercise\", \"reading\", \"lecture\"]\n" +
        "  }\n" +
        "}\n\n" +
        "要求：\n" +
        "1. 根据学生的知识短板和学习需求，重点制定针对性教学计划\n" +
        "2. 资源类型从 mindmap/ppt/exercise/reading/lecture 中选择合适的\n" +
        "3. 每个建议要具体可执行";

    public PlannerAgent(@Qualifier("deepseekAiService") AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "PlannerAgent";
    }

    @Override
    public String getRole() {
        return "课程规划师";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        try {
            StringBuilder userInput = new StringBuilder();
            userInput.append("专业：").append(context.getMajor() != null ? context.getMajor() : "未指定").append("\n");
            userInput.append("课程内容：").append(context.getCourseName() != null ? context.getCourseName() : "未指定").append("\n");
            userInput.append("知识短板：").append(context.getKnowledgeGaps() != null ? context.getKnowledgeGaps() : "无").append("\n");
            userInput.append("学习需求：").append(context.getLearningNeeds() != null ? context.getLearningNeeds() : "无").append("\n");

            if (context.getProfile() != null && context.getProfile().getOverallLevel() != null) {
                userInput.append("学生水平：").append(context.getProfile().getOverallLevel()).append("\n");
            }

            log.info("[PlannerAgent] 开始规划: course={}", context.getCourseName());

            String response = aiService.chat(PLANNER_SYSTEM_PROMPT, userInput.toString(), null);

            String json = extractJson(response);
            Map<String, Object> planData = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            Map<String, Object> plan = (Map<String, Object>) planData.getOrDefault("plan", planData);

            context.put("plan", plan);
            context.put("topic", plan.getOrDefault("topic", context.getCourseName()));
            context.put("teachingGoals", plan.getOrDefault("teachingGoals", List.of()));
            context.put("keyPoints", plan.getOrDefault("keyPoints", List.of()));
            context.put("difficultyPoints", plan.getOrDefault("difficultyPoints", List.of()));
            context.put("suggestedStructure", plan.getOrDefault("suggestedStructure", List.of()));

            String planSummary = "已为「" + plan.getOrDefault("topic", context.getCourseName())
                + "」制定教学计划，包含" + ((List<?>) plan.getOrDefault("keyPoints", List.of())).size()
                + "个重点和" + ((List<?>) plan.getOrDefault("difficultyPoints", List.of())).size() + "个难点";

            log.info("[PlannerAgent] 规划完成: {}", planSummary);

            return AgentResult.ok(planSummary, Map.of("plan", plan));

        } catch (Exception e) {
            log.error("[PlannerAgent] 规划失败", e);
            return AgentResult.fail("课程规划失败: " + e.getMessage());
        }
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
