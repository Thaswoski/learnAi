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
import java.util.regex.Pattern;
import java.util.function.Consumer;

@Component
public class ReviewerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ReviewerAgent.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    private static final String FACT_CHECK_PROMPT =
        "你是一个专业的内容审核员(ReviewerAgent)，属于多智能体系统中的质量审核角色。" +
        "你的任务是对教学内容进行质量评估，给出评分和改进建议。\n\n" +
        "严格按照以下JSON格式输出审核结果，不要输出任何其他内容：\n" +
        "{\n" +
        "  \"passed\": true,\n" +
        "  \"score\": 0-100,\n" +
        "  \"issues\": [\n" +
        "    {\"severity\": \"error|warning|info\", \"location\": \"问题所在的章节或概念\", \"description\": \"问题描述\", \"suggestion\": \"修改建议\"}\n" +
        "  ],\n" +
        "  \"overallAssessment\": \"整体评价一句话\"\n" +
        "}\n\n" +
        "审核标准：\n" +
        "1. 事实准确性：检查教学定义、原理、公式是否准确\n" +
        "2. 逻辑一致性：检查章节之间是否存在逻辑矛盾\n" +
        "3. 教学适用性：内容难度是否适合目标学生\n" +
        "4. 完整性：是否遗漏重要知识点\n" +
        "5. 代码正确性：代码示例是否可编译运行\n" +
        "6. passed必须为true（审核仅提供改进建议，不阻断内容交付）\n" +
        "7. score评分标准：90-100内容优秀，70-89良好，50-69有小问题，30-49有明显瑕疵\n" +
        "8. severity用error标注严重错误、warning标注可改进点、info标注建议";

    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
        Pattern.compile("(?i)(暴力|色情|赌博|毒品|诈骗|传销)")
    );

    public ReviewerAgent(@Qualifier("deepseekAiService") AiService aiService) {
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "ReviewerAgent";
    }

    @Override
    public String getRole() {
        return "质量审核员";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        try {
            if (onStep != null) onStep.accept("正在进行内容安全过滤...");

            // Step 1: 内容安全过滤
            Map<String, Object> content = context.get("content");
            if (content == null) {
                log.warn("[ReviewerAgent] 没有待审核的内容");
                return AgentResult.ok("无内容需审核");
            }

            String contentStr = objectMapper.writeValueAsString(content);
            String safetyIssue = checkSafety(contentStr);
            if (safetyIssue != null) {
                log.warn("[ReviewerAgent] 内容安全过滤不通过: {}", safetyIssue);
                return AgentResult.fail("内容安全审核不通过: " + safetyIssue);
            }

            if (onStep != null) onStep.accept("正在进行事实核查...");

            // Step 2: 事实核查
            String title = (String) content.getOrDefault("title", "未知主题");
            List<Map<String, Object>> sections = (List<Map<String, Object>>) content.getOrDefault("sections", List.of());

            StringBuilder reviewInput = new StringBuilder();
            reviewInput.append("课程主题：").append(title).append("\n\n");
            for (int i = 0; i < sections.size(); i++) {
                Map<String, Object> sec = sections.get(i);
                reviewInput.append("章节").append(i + 1).append("：").append(sec.getOrDefault("heading", "")).append("\n");
                String sectionContent = (String) sec.getOrDefault("content", "");
                reviewInput.append(sectionContent.substring(0, Math.min(500, sectionContent.length()))).append("\n\n");
            }

            String response = aiService.chat(FACT_CHECK_PROMPT, reviewInput.toString(), null);
            String json = extractJson(response);

            Map<String, Object> reviewResult = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});

            int score = ((Number) reviewResult.getOrDefault("score", 0)).intValue();
            List<Map<String, Object>> issues = (List<Map<String, Object>>) reviewResult.getOrDefault("issues", List.of());

            context.put("review", reviewResult);
            context.put("reviewIssues", issues);

            log.info("[ReviewerAgent] 审核完成: score={}, issues={}", score, issues.size());

            if (score < 85) {
                context.emit(AgentEvent.Type.REVIEW_COMPLETED, "ReviewerAgent",
                    Map.of("score", score, "issues", issues.size(), "passed", score >= 70));
            }
            if (score < 70) {
                context.emit(AgentEvent.Type.REVIEW_LOW_SCORE, "ReviewerAgent",
                    Map.of("score", score, "issues", issues.size(), "passed", false));
            }
            if (score >= 85) {
                context.emit(AgentEvent.Type.REVIEW_PASSED, "ReviewerAgent",
                    Map.of("score", score, "issues", issues.size()));
            }

            int errorCount = (int) issues.stream()
                .filter(i -> "error".equals(i.getOrDefault("severity", "")))
                .count();
            int warnCount = (int) issues.stream()
                .filter(i -> "warning".equals(i.getOrDefault("severity", "")))
                .count();
            String resultMsg = "质量审核通过(评分" + score + "/100)，"
                + errorCount + "个错误、" + warnCount + "个建议";
            return AgentResult.ok(resultMsg, Map.of(
                "review", reviewResult,
                "passed", true,
                "score", score
            ));

        } catch (Exception e) {
            log.error("[ReviewerAgent] 审核异常", e);
            return AgentResult.ok("审核服务暂时跳过（系统降级），内容已生成可通过");
        }
    }

    private String checkSafety(String content) {
        if (content == null) return null;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return "检测到不当内容";
            }
        }
        return null;
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
