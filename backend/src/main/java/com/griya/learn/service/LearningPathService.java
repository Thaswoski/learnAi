package com.griya.learn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.LearningPath;
import com.griya.learn.mapper.LearningPathMapper;
import com.griya.learn.mapper.QuizHistoryMapper;
import com.griya.learn.mapper.QuizMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LearningPathService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathService.class);

    private final QuizHistoryMapper quizHistoryMapper;
    private final QuizMapper quizMapper;
    private final LearningPathMapper learningPathMapper;
    private final AiService deepseekAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PATH_PROMPT =
        "你是一个专业的C语言学习规划师。根据用户的答题数据，生成一份个性化学习路径。\n\n" +
        "严格按以下JSON格式输出，不要输出任何其他内容：\n" +
        "{\n" +
        "  \"steps\": [{\"title\":\"阶段名\",\"status\":\"已完成|进行中|未开始\",\"duration\":小时数," +
        "\"tags\":[\"知识点1\",\"知识点2\"]}],\n" +
        "  \"weeklyPlan\": [{\"day\":\"周一\",\"task\":\"具体任务描述\"," +
        "\"duration\":\"X小时\",\"status\":\"未开始\"}]\n" +
        "}\n\n" +
        "要求：\n" +
        "1. steps 按学习路径从基础到进阶排列，第一阶段应是已完成，第二阶段应是进行中\n" +
        "2. 根据用户已掌握的知识点标记为已完成，薄弱知识点标记为进行中\n" +
        "3. steps 总数3-5个阶段，tags 从 QUESTION_KNOWLEDGE_POINTS 中选取\n" +
        "4. weeklyPlan 必须是7天（周一到周日），每天1-2个具体任务，任务要可执行\n" +
        "5. 未开始的任务标记为\"未开始\"\n" +
        "6. 每天总时长不超过3小时";

    public LearningPathService(QuizHistoryMapper quizHistoryMapper,
                                QuizMapper quizMapper,
                                LearningPathMapper learningPathMapper,
                                @Qualifier("deepseekAiService") AiService deepseekAiService) {
        this.quizHistoryMapper = quizHistoryMapper;
        this.quizMapper = quizMapper;
        this.learningPathMapper = learningPathMapper;
        this.deepseekAiService = deepseekAiService;
    }

    public Map<String, Object> generatePath(Long userId) {
        int total = quizHistoryMapper.totalCount(userId);

        // check cache
        LearningPath cache = learningPathMapper.selectByUser(userId);
        if (cache != null && cache.getQuizTotal() != null
                && cache.getQuizTotal().intValue() == total && total > 0) {
            try {
                log.info("[PATH] 命中缓存 userId={}, total={}", userId, total);
                return objectMapper.readValue(cache.getDataJson(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("[PATH] 缓存解析失败，重新生成: {}", e.getMessage());
            }
        }

        // rebuild
        log.info("[PATH] 重新生成 userId={}, total={}", userId, total);
        try {
            Map<String, Object> result = doGeneratePath(userId, total);
            String json = objectMapper.writeValueAsString(result);
            learningPathMapper.upsert(userId, total, json);
            log.info("[PATH] 缓存已更新 userId={}, jsonLen={}", userId, json.length());
            return result;
        } catch (Exception e) {
            log.error("[PATH] DeepSeek调用失败，返回默认路径: {}", e.getMessage());
            return getDefaultPath();
        }
    }

    private Map<String, Object> doGeneratePath(Long userId, int total) {
        try {
            int correct = quizHistoryMapper.correctCount(userId);
            List<Map<String, Object>> knowledgeMastery = buildKnowledgeSummary(userId);
            List<String> allPoints = quizMapper.selectAllKnowledgePoints();

            StringBuilder userData = new StringBuilder();
            userData.append("用户答题统计:\n");
            userData.append("- 总答题数: ").append(total).append("\n");
            userData.append("- 正确数: ").append(correct).append("\n");
            userData.append("- 正确率: ").append(total > 0
                ? String.format("%.1f%%", correct * 100.0 / total) : "0%").append("\n\n");

            userData.append("各知识点掌握度:\n");
            for (Map<String, Object> km : knowledgeMastery) {
                userData.append("- ").append(km.get("name"))
                    .append(": ").append(km.get("mastery")).append("% (")
                    .append(km.get("correct")).append("/").append(km.get("total")).append(")\n");
            }

            userData.append("\n题库全部知识点: ").append(String.join(", ", allPoints)).append("\n");

            String aiResponse = deepseekAiService.chat(PATH_PROMPT, userData.toString(), null);
            log.info("[PATH] AI返回长度: {}", aiResponse != null ? aiResponse.length() : 0);

            String json = extractJson(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(json,
                new TypeReference<Map<String, Object>>() {});

            List<Map<String, Object>> steps = (List<Map<String, Object>>) parsed.get("steps");
            List<Map<String, Object>> weeklyPlan = (List<Map<String, Object>>) parsed.get("weeklyPlan");

            for (Map<String, Object> step : steps) {
                String status = (String) step.getOrDefault("status", "未开始");
                step.put("statusClass", statusToClass(status));
            }

            for (Map<String, Object> plan : weeklyPlan) {
                String status = (String) plan.getOrDefault("status", "未开始");
                plan.put("statusClass", statusToClass(status));
            }

            for (Map<String, Object> step : steps) {
                List<String> tags = (List<String>) step.get("tags");
                if (tags != null && !tags.isEmpty()) {
                    step.put("tagClass", "badge-blue");
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("steps", steps);
            result.put("weeklyPlan", weeklyPlan);
            return result;
        } catch (Exception e) {
            log.error("[PATH] AI解析失败: {}", e.getMessage());
            return getDefaultPath();
        }
    }

    private List<Map<String, Object>> buildKnowledgeSummary(Long userId) {
        List<Map<String, Object>> raw = quizHistoryMapper.knowledgePointMastery(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row.get("knowledgePoint"));
            item.put("total", row.get("total"));
            item.put("correct", row.get("correct"));
            Number tN = (Number) row.get("total");
            Number cN = (Number) row.get("correct");
            int t = tN != null ? tN.intValue() : 0;
            int c = cN != null ? cN.intValue() : 0;
            item.put("mastery", t > 0 ? c * 100 / t : 0);
            result.add(item);
        }
        return result;
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        text = text.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "{}";
    }

    private String statusToClass(String status) {
        if ("已完成".equals(status)) return "badge-green";
        if ("进行中".equals(status)) return "badge-orange";
        return "badge-blue";
    }

    private Map<String, Object> getDefaultPath() {
        List<Map<String, Object>> steps = List.of(
            Map.of("title", "基本语法与数据类型", "status", "已完成", "statusClass", "badge-green",
                "duration", 6, "tags", List.of("printf/scanf", "变量与常量", "数据类型转换"), "tagClass", "badge-green"),
            Map.of("title", "运算符与控制结构", "status", "进行中", "statusClass", "badge-orange",
                "duration", 10, "tags", List.of("条件语句", "循环结构", "逻辑运算符"), "tagClass", "badge-blue"),
            Map.of("title", "函数与模块化", "status", "未开始", "statusClass", "badge-blue",
                "duration", 8, "tags", List.of("函数定义", "参数传递", "递归调用"), "tagClass", "badge-blue"),
            Map.of("title", "数组与指针", "status", "未开始", "statusClass", "badge-blue",
                "duration", 14, "tags", List.of("一维数组", "指针运算", "动态内存"), "tagClass", "badge-blue"),
            Map.of("title", "结构体与文件操作", "status", "未开始", "statusClass", "badge-blue",
                "duration", 10, "tags", List.of("结构体定义", "文件读写", "综合项目"), "tagClass", "badge-blue")
        );

        List<Map<String, Object>> weeklyPlan = List.of(
            Map.of("day", "周一", "task", "复习条件语句if-else与switch", "duration", "1.5小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周二", "task", "练习for/while/do-while循环结构", "duration", "2小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周三", "task", "逻辑运算符与短路求值练习", "duration", "1.5小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周四", "task", "运算符与控制结构综合练习", "duration", "2小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周五", "task", "函数定义与调用入门", "duration", "2小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周六", "task", "参数传递：值传递与地址传递", "duration", "2.5小时", "status", "未开始", "statusClass", "badge-blue"),
            Map.of("day", "周日", "task", "本周知识回顾与错题重做", "duration", "1.5小时", "status", "未开始", "statusClass", "badge-blue")
        );

        return Map.of("steps", steps, "weeklyPlan", weeklyPlan);
    }
}
