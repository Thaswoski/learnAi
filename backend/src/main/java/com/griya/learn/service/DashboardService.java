package com.griya.learn.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final QuizHistoryMapper quizHistoryMapper;
    private final QuizMapper quizMapper;
    private final ResourceRecordMapper resourceRecordMapper;
    private final LearningPathMapper learningPathMapper;
    private final DashboardCacheMapper dashboardCacheMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardService(QuizHistoryMapper quizHistoryMapper, QuizMapper quizMapper,
                             ResourceRecordMapper resourceRecordMapper,
                             LearningPathMapper learningPathMapper,
                             DashboardCacheMapper dashboardCacheMapper) {
        this.quizHistoryMapper = quizHistoryMapper;
        this.quizMapper = quizMapper;
        this.resourceRecordMapper = resourceRecordMapper;
        this.learningPathMapper = learningPathMapper;
        this.dashboardCacheMapper = dashboardCacheMapper;
    }

    public Map<String, Object> getDashboard(Long userId) {
        int quizTotal = quizHistoryMapper.totalCount(userId);
        int resourceTotal = resourceRecordMapper.countByUser(userId);

        // check cache
        var cache = dashboardCacheMapper.selectByUser(userId);
        if (cache != null && cache.getQuizTotal() != null && cache.getResourceTotal() != null
                && cache.getQuizTotal().intValue() == quizTotal
                && cache.getResourceTotal().intValue() == resourceTotal
                && quizTotal + resourceTotal > 0) {
            try {
                log.info("[DASHBOARD] 命中缓存 userId={}", userId);
                return objectMapper.readValue(cache.getDataJson(),
                    new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("[DASHBOARD] 缓存解析失败，重新计算: {}", e.getMessage());
            }
        }

        // rebuild
        log.info("[DASHBOARD] 重新计算 userId={}, quiz={}, resource={}", userId, quizTotal, resourceTotal);
        Map<String, Object> result = buildDashboard(userId, quizTotal, resourceTotal);
        try {
            String json = objectMapper.writeValueAsString(result);
            dashboardCacheMapper.upsert(userId, quizTotal, resourceTotal, json);
            log.info("[DASHBOARD] 缓存已更新 userId={}, jsonLen={}", userId, json.length());
        } catch (Exception e) {
            log.error("[DASHBOARD] 写入缓存失败: {}", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> buildDashboard(Long userId, int quizTotal, int resourceTotal) {
        Map<String, Object> result = new LinkedHashMap<>();

        int todayAnswers = quizHistoryMapper.todayCount(userId);
        int todayCorrect = quizHistoryMapper.todayCorrectCount(userId);
        int totalCorrect = quizHistoryMapper.correctCount(userId);
        int totalQuestions = quizMapper.countTotal();

        result.put("stats", buildStats(todayAnswers, totalCorrect, resourceTotal, quizTotal, totalQuestions));
        result.put("todayTasks", buildTodayTasks(userId));
        result.put("weeklyChart", buildWeeklyChart(userId));
        result.put("knowledgeChart", buildKnowledgeChart(userId));

        log.info("[DASHBOARD] userId={}, today={}, total={}, resources={}",
            userId, todayAnswers, quizTotal, resourceTotal);
        return result;
    }

    private List<Map<String, Object>> buildStats(int todayAnswers, int totalCorrect,
                                                   int resourceCount, int totalAnswers,
                                                   int totalQuestions) {
        int progress = totalQuestions > 0 ? Math.min(100, totalAnswers * 100 / totalQuestions) : 0;

        return List.of(
            Map.of("label", "今日答题", "value", todayAnswers + "题",
                "icon", "ri-question-answer-line", "color", "#165DFF", "bg", "#E8F0FE"),
            Map.of("label", "累计正确", "value", String.valueOf(totalCorrect),
                "icon", "ri-check-double-line", "color", "#00B42A", "bg", "#E8FFEA"),
            Map.of("label", "生成资源", "value", resourceCount + "份",
                "icon", "ri-file-copy-line", "color", "#FF7D00", "bg", "#FFF7E8"),
            Map.of("label", "学习进度", "value", progress + "%",
                "icon", "ri-bar-chart-2-line", "color", "#722ED1", "bg", "#F5F0FF")
        );
    }

    private List<Map<String, Object>> buildTodayTasks(Long userId) {
        try {
            var path = learningPathMapper.selectByUser(userId);
            if (path == null || path.getDataJson() == null) return List.of();

            Map<String, Object> data = objectMapper.readValue(path.getDataJson(),
                new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> weeklyPlan = (List<Map<String, Object>>) data.get("weeklyPlan");
            if (weeklyPlan == null) return List.of();

            String today = getTodayChinese();
            List<Map<String, Object>> todayTasks = new ArrayList<>();
            for (Map<String, Object> plan : weeklyPlan) {
                if (today.equals(plan.get("day"))) {
                    Map<String, Object> task = new LinkedHashMap<>();
                    task.put("name", plan.getOrDefault("task", ""));
                    task.put("duration", plan.getOrDefault("duration", ""));
                    task.put("status", plan.getOrDefault("status", "未开始"));
                    task.put("course", "C语言学习");
                    todayTasks.add(task);
                }
            }
            return todayTasks;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String getTodayChinese() {
        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        return switch (dow) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private Map<String, Object> buildWeeklyChart(Long userId) {
        List<Map<String, Object>> raw = quizHistoryMapper.recentWeekDailyCount(userId);

        Map<String, Integer> dateMap = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            dateMap.put(d.format(fmt), 0);
        }
        for (Map<String, Object> row : raw) {
            String date = row.get("date") != null ? row.get("date").toString() : "";
            Number totalN = (Number) row.get("total");
            int count = totalN != null ? totalN.intValue() : 0;
            if (date.length() >= 10) date = date.substring(5, 10);
            dateMap.put(date, count);
        }

        return Map.of(
            "dates", new ArrayList<>(dateMap.keySet()),
            "values", new ArrayList<>(dateMap.values())
        );
    }

    private List<Map<String, Object>> buildKnowledgeChart(Long userId) {
        List<Map<String, Object>> mastery = quizHistoryMapper.knowledgePointMastery(userId);
        int mastered = 0, learning = 0;

        for (Map<String, Object> m : mastery) {
            Number tN = (Number) m.get("total");
            Number cN = (Number) m.get("correct");
            int t = tN != null ? tN.intValue() : 0;
            int c = cN != null ? cN.intValue() : 0;
            int rate = t > 0 ? c * 100 / t : 0;
            if (rate >= 70) mastered++;
            else learning++;
        }

        int totalPoints = quizMapper.selectAllKnowledgePoints().size();
        int unlearned = Math.max(1, totalPoints - mastered - learning);

        return List.of(
            Map.of("value", mastered, "name", "已掌握", "color", "#165DFF"),
            Map.of("value", learning, "name", "学习中", "color", "#3C7EFF"),
            Map.of("value", Math.max(1, unlearned), "name", "未学习", "color", "#C9CDD4")
        );
    }
}
