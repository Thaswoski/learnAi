package com.griya.learn.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.service.AiService;
import com.griya.learn.service.XfSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class ContentAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ContentAgent.class);

    private final AiService aiService;
    private final XfSearchService xfSearchService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
        .configure(com.fasterxml.jackson.core.json.JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(), true);

    private static final String CONTENT_SYSTEM_PROMPT =
        "你是一个专业的内容创作者(ContentAgent)，属于多智能体系统中的内容生成角色。\n" +
        "请根据课程规划师制定的计划生成教学内容。\n\n" +
        "【重要】必须只输出以下JSON格式，JSON中每个字段用双引号包裹，字符串内的双引号必须转义为\\\"：\n\n" +
        "{\n" +
        "  \"title\": \"教学内容标题\",\n" +
        "  \"summary\": \"150字以内的内容摘要\",\n" +
        "  \"sections\": [\n" +
        "    {\"heading\": \"章节标题\", \"content\": \"该章节的教学内容(300-500字)\"}\n" +
        "  ],\n" +
        "  \"exercises\": [\n" +
        "    {\"question\": \"题目\", \"answer\": \"答案\", \"difficulty\": \"easy|medium|hard\"}\n" +
        "  ]\n" +
        "}\n\n" +
        "要求：\n" +
        "1. sections共3个章节，每个chapter的content控制在300-500字\n" +
        "2. exercises共3道题\n" +
        "3. JSON字符串内不要出现未转义的双引号\n" +
        "4. 代码示例放在chapter的content中用三反引号包裹";

    public ContentAgent(@Qualifier("deepseekAiService") AiService aiService,
                         XfSearchService xfSearchService) {
        this.aiService = aiService;
        this.xfSearchService = xfSearchService;
    }

    @Override
    public String getName() {
        return "ContentAgent";
    }

    @Override
    public String getRole() {
        return "内容创作者";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        Map<String, Object> contentData = null;

        try {
            if (onStep != null) onStep.accept("正在搜索相关教学资料...");
            String searchText = doSearch(context);

            Map<String, Object> plan = context.get("plan");
            List<String> keyPoints = context.get("keyPoints");
            List<String> difficultyPoints = context.get("difficultyPoints");
            String topic = context.get("topic");

            StringBuilder prompt = new StringBuilder();
            prompt.append("课程主题：").append(topic != null ? topic : context.getCourseName()).append("\n");
            prompt.append("专业：").append(context.getMajor() != null ? context.getMajor() : "").append("\n");
            prompt.append("知识短板：").append(context.getKnowledgeGaps() != null ? context.getKnowledgeGaps() : "").append("\n");
            prompt.append("学习需求：").append(context.getLearningNeeds() != null ? context.getLearningNeeds() : "").append("\n");

            if (keyPoints != null) {
                prompt.append("教学重点：").append(String.join("、", keyPoints)).append("\n");
            }
            if (difficultyPoints != null) {
                prompt.append("教学难点：").append(String.join("、", difficultyPoints)).append("\n");
            }

            if (!searchText.isEmpty()) {
                prompt.append("\n搜索到的参考资料：\n").append(searchText);
            }

            log.info("[ContentAgent] 开始生成内容: topic={}, resourceType={}",
                topic, context.getResourceType());

            if (onStep != null) onStep.accept("正在生成教学内容...");

            String response = aiService.chat(CONTENT_SYSTEM_PROMPT, prompt.toString(), null);

            // 多策略JSON解析
            contentData = tryParseJson(response);

        } catch (Exception e) {
            log.error("[ContentAgent] 内容生成失败", e);
        }

        if (contentData == null) {
            contentData = buildFallback(context.getCourseName());
        }

        context.put("content", contentData);
        context.put("title", contentData.getOrDefault("title", context.getCourseName()));

        int sectionCount = ((List<?>) contentData.getOrDefault("sections", List.of())).size();
        int exerciseCount = ((List<?>) contentData.getOrDefault("exercises", List.of())).size();

        String summary = "已生成「" + contentData.getOrDefault("title", context.getCourseName())
            + "」教学内容，包含" + sectionCount + "个章节、" + exerciseCount + "道练习题";

        log.info("[ContentAgent] 内容生成完成: sections={}, exercises={}", sectionCount, exerciseCount);

        return AgentResult.ok(summary, Map.of("content", contentData));
    }

    private Map<String, Object> tryParseJson(String rawResponse) {
        if (rawResponse == null) return null;

        // 策略1: 直接提取并解析
        String json = extractBracketed(rawResponse);
        if (json != null) {
            Map<String, Object> result = attemptParse(json);
            if (result != null) return result;
        }

        // 策略2: 清理后再解析
        String cleaned = cleanForJson(rawResponse);
        if (cleaned != null) {
            Map<String, Object> result = attemptParse(cleaned);
            if (result != null) return result;
        }

        // 策略3: 兜底 — 把原始文本当做一个章节
        log.warn("[ContentAgent] 多策略JSON解析均失败，使用原始文本兜底");
        return buildFallbackFromText(rawResponse);
    }

    private String extractBracketed(String text) {
        text = text.trim();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (text.charAt(i) == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private Map<String, Object> attemptParse(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("[ContentAgent] JSON解析尝试失败: {}", e.getMessage());
            return null;
        }
    }

    private String cleanForJson(String text) {
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                sb.append(c);
            } else if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if ("\"\\/bfnrtu".indexOf(next) >= 0) {
                    sb.append('\\').append(next);
                    i++;
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

    private String doSearch(AgentContext context) {
        String query = context.getCourseName();
        if (query == null || query.isEmpty()) return "";

        String searchText = xfSearchService.searchToText(query, 6);
        if (!searchText.isEmpty()) {
            log.info("[ContentAgent] ONE SEARCH 获取到搜索结果, query={}", query);
            return searchText;
        }
        return "";
    }

    private Map<String, Object> buildFallback(String courseName) {
        Map<String, Object> fb = new LinkedHashMap<>();
        fb.put("title", courseName != null ? courseName : "教学内容");
        fb.put("summary", "由AI生成的个性化教学内容");
        fb.put("sections", List.of(Map.of(
            "heading", "开始学习",
            "content", "教学内容已生成，请查看下方内容。"
        )));
        fb.put("exercises", List.of());
        return fb;
    }

    private Map<String, Object> buildFallbackFromText(String rawResponse) {
        Map<String, Object> fb = new LinkedHashMap<>();
        fb.put("title", "教学资源");
        fb.put("summary", "由AI生成的个性化教学内容");
        String text = rawResponse != null
            ? rawResponse.replaceAll("[{}\\[\\]\"]", "").replaceAll("[\\n\\r]+", "\n").trim()
            : "内容生成中...";
        if (text.length() > 2000) text = text.substring(0, 2000) + "...";
        List<Map<String, Object>> sections = new ArrayList<>();

        String[] parts = text.split("(?=第[一二三四五六七八九十\\d]+章|第[一二三四五六七八九十\\d]+节|###|##|(?m)^\\d+[\\.、])");
        if (parts.length <= 1) {
            sections.add(Map.of("heading", "教学内容", "content", text));
        } else {
            for (int i = 0; i < Math.min(parts.length, 6); i++) {
                String p = parts[i].trim();
                if (p.isEmpty()) continue;
                int nl = p.indexOf('\n');
                String heading = nl > 0 ? p.substring(0, Math.min(nl, 40)) : p.substring(0, Math.min(p.length(), 40));
                sections.add(Map.of("heading", heading, "content", p));
            }
        }

        fb.put("sections", sections);
        fb.put("exercises", List.of());
        return fb;
    }
}
