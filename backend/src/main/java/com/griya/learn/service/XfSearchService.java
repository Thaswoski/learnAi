package com.griya.learn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class XfSearchService {

    private static final Logger log = LoggerFactory.getLogger(XfSearchService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${xf.search.api-password:}")
    private String apiPassword;

    @Value("${xf.search.url:https://search-api-open.cn-huabei-1.xf-yun.com/v2/search}")
    private String searchUrl;

    public List<Map<String, String>> search(String query, int limit) {
        if (apiPassword == null || apiPassword.isEmpty()) {
            log.warn("[XF-SEARCH] APIPassword 未配置，跳过搜索");
            return List.of();
        }

        try {
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("query", query);
            searchParams.put("limit", limit);

            Map<String, Object> enhance = new HashMap<>();
            enhance.put("open_full_text", true);
            enhance.put("open_rerank", true);
            searchParams.put("enhance", enhance);

            body.put("search_params", searchParams);

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiPassword)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            log.info("[XF-SEARCH] 请求: query={}, limit={}", query, limit);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            log.info("[XF-SEARCH] HTTP status={}", status);

            if (status != 200) {
                log.error("[XF-SEARCH] HTTP 错误: {}, body={}", status,
                    response.body().substring(0, Math.min(500, response.body().length())));
                return List.of();
            }

            Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);

            boolean success = Boolean.TRUE.equals(data.get("success"));
            String errCode = (String) data.get("err_code");
            log.info("[XF-SEARCH] success={}, err_code={}", success, errCode);

            if (!success) {
                log.error("[XF-SEARCH] 搜索失败: err_code={}, message={}",
                    errCode, data.get("message"));
                return List.of();
            }

            Map<String, Object> dataObj = (Map<String, Object>) data.get("data");
            if (dataObj == null) return List.of();

            Map<String, Object> searchResults = (Map<String, Object>) dataObj.get("search_results");
            if (searchResults == null) return List.of();

            List<Map<String, Object>> documents = (List<Map<String, Object>>) searchResults.get("documents");
            if (documents == null) return List.of();

            List<Map<String, String>> results = new ArrayList<>();
            for (Map<String, Object> doc : documents) {
                Map<String, String> item = new HashMap<>();
                item.put("name", (String) doc.getOrDefault("name", "无标题"));
                item.put("url", (String) doc.getOrDefault("url", ""));
                item.put("summary", valueOrEmpty(doc, "summary", "content"));
                results.add(item);
            }

            log.info("[XF-SEARCH] ✓ 成功: {} 条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("[XF-SEARCH] 异常: {}", e.getMessage());
            return List.of();
        }
    }

    public String searchToText(String query, int limit) {
        List<Map<String, String>> results = search(query, limit);
        if (results.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n以下是从全网搜索到的相关资料：\n");
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> item = results.get(i);
            sb.append(i + 1).append(". ").append(item.get("name")).append("\n");
            String summary = item.get("summary");
            if (summary != null && !summary.isEmpty()) {
                sb.append("   摘要：").append(
                    summary.length() > 200 ? summary.substring(0, 200) + "..." : summary
                ).append("\n");
            }
        }
        return sb.toString();
    }

    private String valueOrEmpty(Map<String, Object> doc, String... keys) {
        for (String key : keys) {
            Object val = doc.get(key);
            if (val instanceof String s && !s.isEmpty()) return s;
        }
        return "无摘要";
    }
}
