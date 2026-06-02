package com.griya.learn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.EvaluationCache;
import com.griya.learn.mapper.EvaluationCacheMapper;
import com.griya.learn.mapper.QuizHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final QuizHistoryMapper quizHistoryMapper;
    private final EvaluationCacheMapper evaluationCacheMapper;
    private final AiService deepseekAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SUGGESTION_PROMPT =
        "你是一个专业的C语言学习评估顾问。根据用户的答题数据，生成3-4条个性化改进建议。\n\n" +
        "严格按以下JSON数组格式输出，不要输出任何其他内容：\n" +
        "[{\"type\":\"warning|info|success\",\"title\":\"建议标题(10字内)\",\"content\":\"具体建议(50字内)\"}]\n\n" +
        "要求：\n" +
        "1. type取warning(需警惕)/info(提示)/success(做得好)\n" +
        "2. 建议要具体、可操作，针对用户最弱的知识点给出练习方向\n" +
        "3. 条数控制在3-4条";

    private static final Map<String, int[]> KNOWLEDGE_TO_DIMENSION = Map.ofEntries(
        Map.entry("基本语法与数据类型", new int[]{5, 3, 1, 1, 0}),
        Map.entry("运算符与表达式",    new int[]{3, 3, 4, 2, 1}),
        Map.entry("控制结构",          new int[]{3, 4, 4, 2, 1}),
        Map.entry("函数",              new int[]{2, 5, 4, 3, 2}),
        Map.entry("数组",              new int[]{2, 5, 4, 3, 2}),
        Map.entry("指针",              new int[]{2, 5, 5, 4, 3}),
        Map.entry("字符串",            new int[]{2, 4, 3, 3, 2}),
        Map.entry("结构体",            new int[]{3, 4, 3, 4, 2}),
        Map.entry("文件操作",          new int[]{2, 3, 2, 4, 2}),
        Map.entry("动态内存管理",      new int[]{2, 5, 5, 4, 3}),
        Map.entry("预处理",            new int[]{3, 2, 2, 2, 1}),
        Map.entry("位运算",            new int[]{3, 2, 4, 2, 2})
    );

    private static final String[] DIMENSION_NAMES = {
        "理论理解", "编程实现", "问题解决", "知识应用", "创新思维"
    };

    private static final int MIN_QUESTIONS = 10;

    public EvaluationService(QuizHistoryMapper quizHistoryMapper,
                              EvaluationCacheMapper evaluationCacheMapper,
                              @Qualifier("deepseekAiService") AiService deepseekAiService) {
        this.quizHistoryMapper = quizHistoryMapper;
        this.evaluationCacheMapper = evaluationCacheMapper;
        this.deepseekAiService = deepseekAiService;
    }

    public Map<String, Object> getEvaluation(Long userId) {
        int total = quizHistoryMapper.totalCount(userId);

        if (total < MIN_QUESTIONS) {
            return buildEmptyResult(total);
        }

        // check cache
        EvaluationCache cache = evaluationCacheMapper.selectByUser(userId);
        if (cache != null && cache.getQuizTotal() != null
                && cache.getQuizTotal().intValue() == total) {
            try {
                log.info("[EVAL] 命中缓存 userId={}, total={}", userId, total);
                return objectMapper.readValue(cache.getDataJson(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("[EVAL] 缓存解析失败，重新计算: {}", e.getMessage());
            }
        }

        // rebuild
        log.info("[EVAL] 重新计算 userId={}, total={}", userId, total);
        Map<String, Object> result = buildEvaluation(userId, total);
        try {
            String json = objectMapper.writeValueAsString(result);
            evaluationCacheMapper.upsert(userId, total, json);
            log.info("[EVAL] 缓存已更新 userId={}, jsonLen={}", userId, json.length());
        } catch (Exception e) {
            log.error("[EVAL] 写入缓存失败: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> buildEmptyResult(int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", false);
        result.put("totalQuestions", total);
        result.put("requiredQuestions", MIN_QUESTIONS);
        result.put("overallScore", 0);
        result.put("accuracyRate", 0.0);
        result.put("averageMastery", 0);
        result.put("knowledgeMastery", List.of());
        result.put("dimensionNames", DIMENSION_NAMES);
        result.put("dimensionValues", new int[]{0, 0, 0, 0, 0});
        result.put("weeklyTrend", Map.of("weeks", List.of(), "values", List.of()));
        result.put("suggestions", List.of());
        log.info("[EVAL] userId 数据不足, total={} < {}", total, MIN_QUESTIONS);
        return result;
    }

    private Map<String, Object> buildEvaluation(Long userId, int total) {
        Map<String, Object> result = new LinkedHashMap<>();
        int correct = quizHistoryMapper.correctCount(userId);

        result.put("ready", true);
        result.put("totalQuestions", total);
        result.put("requiredQuestions", MIN_QUESTIONS);

        // 1. 综合得分
        result.put("overallScore", total > 0
            ? new BigDecimal(correct).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP).intValue()
            : 0);

        result.put("accuracyRate", total > 0
            ? new BigDecimal(correct).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue()
            : 0.0);

        // 2. 各维度能力评估 (radar chart data)
        List<Map<String, Object>> knowledgeMastery = buildKnowledgeMastery(userId);
        result.put("knowledgeMastery", knowledgeMastery);

        int[] dimensionValues = new int[5];
        int[] dimensionWeights = new int[5];
        for (Map<String, Object> km : knowledgeMastery) {
            String kp = (String) km.get("name");
            double mastery = ((Number) km.get("mastery")).doubleValue();
            int[] contrib = KNOWLEDGE_TO_DIMENSION.getOrDefault(kp, new int[]{1, 1, 1, 1, 1});
            for (int i = 0; i < 5; i++) {
                dimensionValues[i] += (int) (mastery * contrib[i]);
                dimensionWeights[i] += contrib[i];
            }
        }

        int[] normalized = new int[5];
        for (int i = 0; i < 5; i++) {
            normalized[i] = dimensionWeights[i] > 0
                ? dimensionValues[i] / dimensionWeights[i]
                : 0;
        }
        result.put("dimensionNames", DIMENSION_NAMES);
        result.put("dimensionValues", normalized);

        // 3. 知识掌握率
        double avgMastery = knowledgeMastery.stream()
            .mapToDouble(k -> ((Number) k.get("mastery")).doubleValue())
            .average().orElse(0);
        result.put("averageMastery", Math.round(avgMastery));

        // 4. 学习效率 (based on overall activity)
        if (total > 0) {
            int compileErrors = quizHistoryMapper.totalCount(userId); // approximate
            result.put("learningEfficiency", Math.min(95, 50 + (int) (correct * 45.0 / Math.max(total, 1))));
        } else {
            result.put("learningEfficiency", 0);
        }

        // 5. 每周趋势
        List<Map<String, Object>> weeklyRaw = quizHistoryMapper.weeklyStats(userId, 6);
        result.put("weeklyTrend", buildWeeklyTrend(weeklyRaw));

        // 6. 个性化建议
        result.put("suggestions", generateSuggestions(normalized, knowledgeMastery, avgMastery));

        log.info("[EVAL] userId={}, total={}, correct={}, score={}",
            userId, total, correct, result.get("overallScore"));
        return result;
    }

    private List<Map<String, Object>> buildKnowledgeMastery(Long userId) {
        List<Map<String, Object>> raw = quizHistoryMapper.knowledgePointMastery(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            String kp = (String) row.get("knowledgePoint");
            Number totalN = (Number) row.get("total");
            Number correctN = (Number) row.get("correct");
            int t = totalN != null ? totalN.intValue() : 0;
            int c = correctN != null ? correctN.intValue() : 0;

            int mastery = t > 0
                ? new BigDecimal(c).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(t), 0, RoundingMode.HALF_UP).intValue()
                : 0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", kp);
            item.put("mastery", mastery);
            item.put("total", t);
            item.put("correct", c);
            item.put("suggestion", suggestForMastery(mastery));
            result.add(item);
        }
        return result;
    }

    private String suggestForMastery(int mastery) {
        if (mastery >= 80) return "保持现有水平";
        if (mastery >= 60) return "多做专题练习";
        return "系统复习+重点突破";
    }

    private Object buildWeeklyTrend(List<Map<String, Object>> raw) {
        List<String> weeks = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            String week = row.get("week") != null ? row.get("week").toString() : "";
            Number totalN = (Number) row.get("total");
            Number correctN = (Number) row.get("correct");
            int t = totalN != null ? totalN.intValue() : 0;
            int c = correctN != null ? correctN.intValue() : 0;

            weeks.add(week);
            values.add(t > 0
                ? new BigDecimal(c).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(t), 0, RoundingMode.HALF_UP).intValue()
                : 0);
        }

        return Map.of("weeks", weeks, "values", values);
    }

    private List<Map<String, Object>> generateSuggestions(int[] dimensions,
                                                           List<Map<String, Object>> knowledgeMastery,
                                                           double avgMastery) {
        try {
            StringBuilder dataJson = new StringBuilder();
            dataJson.append("{\n");
            dataJson.append("  \"overallScore\": ").append(
                (int) knowledgeMastery.stream().mapToDouble(k -> ((Number) k.get("mastery")).doubleValue())
                    .average().orElse(0)).append(",\n");
            dataJson.append("  \"averageMastery\": ").append((int) avgMastery).append(",\n");
            dataJson.append("  \"dimensions\": {");
            for (int i = 0; i < 5; i++) {
                dataJson.append("\"").append(DIMENSION_NAMES[i]).append("\": ").append(dimensions[i]);
                if (i < 4) dataJson.append(", ");
            }
            dataJson.append("},\n");
            dataJson.append("  \"knowledgeMastery\": [");
            for (int i = 0; i < Math.min(knowledgeMastery.size(), 6); i++) {
                Map<String, Object> k = knowledgeMastery.get(i);
                dataJson.append("{\"name\":\"").append(k.get("name"))
                    .append("\",\"mastery\":").append(k.get("mastery")).append("}");
                if (i < Math.min(knowledgeMastery.size(), 6) - 1) dataJson.append(", ");
            }
            dataJson.append("]\n}");

            String aiResponse = deepseekAiService.chat(SUGGESTION_PROMPT, dataJson.toString(), null);
            log.info("[EVAL-SUGGEST] AI返回长度: {}", aiResponse != null ? aiResponse.length() : 0);

            String json = extractJson(aiResponse);
            List<Map<String, Object>> aiSuggestions = objectMapper.readValue(json,
                new TypeReference<List<Map<String, Object>>>() {});
            log.info("[EVAL-SUGGEST] AI生成 {} 条建议", aiSuggestions.size());
            return aiSuggestions;

        } catch (Exception e) {
            log.error("[EVAL-SUGGEST] DeepSeek调用失败，回退到静态建议: {}", e.getMessage());
            return fallbackSuggestions(dimensions, knowledgeMastery, avgMastery);
        }
    }

    private String extractJson(String text) {
        if (text == null) return "[]";
        text = text.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "[]";
    }

    private List<Map<String, Object>> fallbackSuggestions(int[] dimensions,
                                                           List<Map<String, Object>> knowledgeMastery,
                                                           double avgMastery) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        int minIdx = 0;
        for (int i = 1; i < 5; i++) {
            if (dimensions[i] < dimensions[minIdx]) minIdx = i;
        }

        Map<String, Object> s1 = new LinkedHashMap<>();
        s1.put("type", "warning");
        s1.put("title", DIMENSION_NAMES[minIdx] + "需要加强");
        s1.put("content", "你的" + DIMENSION_NAMES[minIdx] + "得分相对较低（" + dimensions[minIdx]
            + "分），建议重点练习相关题型。");
        suggestions.add(s1);

        knowledgeMastery.stream()
            .filter(k -> ((Number) k.get("mastery")).doubleValue() < 50)
            .findFirst()
            .ifPresent(k -> {
                Map<String, Object> s2 = new LinkedHashMap<>();
                s2.put("type", "info");
                s2.put("title", "重点复习「" + k.get("name") + "」");
                s2.put("content", "该知识点掌握度仅 " + k.get("mastery")
                    + "%，建议系统学习后多加练习。");
                suggestions.add(s2);
            });

        if (avgMastery < 60) {
            Map<String, Object> s3 = new LinkedHashMap<>();
            s3.put("type", "warning");
            s3.put("title", "调整学习节奏");
            s3.put("content", "整体掌握率偏低，建议放慢节奏，逐个知识点突破。");
            suggestions.add(s3);
        } else {
            Map<String, Object> s3 = new LinkedHashMap<>();
            s3.put("type", "success");
            s3.put("title", "保持学习节奏");
            s3.put("content", "你正处于稳步提升阶段，建议保持每天至少1小时的练习。");
            suggestions.add(s3);
        }

        return suggestions;
    }
}
