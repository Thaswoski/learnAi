package com.griya.learn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service("qwenAiService")
public class QwenAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(QwenAiService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${qwen.api-key}")
    private String apiKey;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${qwen.model:qwen3.6-plus}")
    private String model;

    private static final String TUTOR_SYSTEM_PROMPT =
        "你是一个专业的AI智能辅导老师，名为智多星。你的任务是帮助学生解决学习中的各种问题，包括但不限于：" +
        "1. 解答课程知识点的疑问 2. 讲解算法和编程概念 3. 辅导作业和习题 " +
        "4. 提供学习方法建议 5. 分析错题原因 6. 推荐学习资源。" +
        "如果用户上传了图片（如题目截图、代码截图、笔记等），请仔细分析图片内容并给出解答。" +
        "请用亲切、耐心的语气回复，像一位真正的老师一样引导学生思考，而不是直接给答案。" +
        "回答时适当使用Markdown格式让内容更清晰（标题、列表、代码块等），但不要过于冗长。" +
        "如果学生问的问题你不确定，请诚实地说明并提供进一步学习的建议。";

    public String getModelName() { return "千问 (" + model + ")"; }

    public boolean supportsImage() { return true; }

    @Override
    public String chat(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", (Object) systemPrompt));
            messages.add(Map.of("role", "user", "content", (Object) userMessage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", true);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4096);

            log.info("调用千问通用: model={}, messageCount={}", model, messages.size());

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("千问通用 API error {}: {}", response.statusCode(), errBody);
                return "AI服务请求失败(HTTP " + response.statusCode() + ")，请检查API Key和模型名称";
            }

            StringBuilder fullContent = new StringBuilder();
            int chunkCount = 0;
            int contentChunkCount = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;
                        try {
                            Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null) {
                                    Object contentObj = delta.get("content");
                                    if (contentObj != null) {
                                        String content = (String) contentObj;
                                        if (!content.isEmpty()) {
                                            fullContent.append(content);
                                            contentChunkCount++;
                                            if (onChunk != null) onChunk.accept(content);
                                        }
                                    }
                                }
                            }
                            chunkCount++;
                        } catch (Exception e) {
                            log.debug("SSE解析跳过一行: {}", e.getMessage());
                        }
                    }
                }
            }

            log.info("千问通用响应完成: totalChunks={}, contentChunks={}, contentLength={}",
                chunkCount, contentChunkCount, fullContent.length());

            if (fullContent.length() == 0 && chunkCount > 0) {
                return "AI思考时间较长但未生成有效回复，请重试或简化提问。";
            }
            if (fullContent.length() == 0) {
                return "AI未返回内容，请检查API Key是否有权限访问模型 " + model;
            }

            return fullContent.toString();

        } catch (Exception e) {
            log.error("千问通用调用异常", e);
            return "AI服务连接失败: " + e.getMessage();
        }
    }

    public String tutorChat(String userMessage, String base64Image, List<Map<String, String>> history, Consumer<String> onChunk) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            messages.add(Map.of("role", "system", "content", TUTOR_SYSTEM_PROMPT));

            if (history != null) {
                for (Map<String, String> h : history) {
                    messages.add(Map.of("role", (Object) h.get("role"), "content", (Object) h.get("content")));
                }
            }

            if (base64Image != null && !base64Image.isEmpty()) {
                List<Map<String, Object>> contentParts = new ArrayList<>();
                contentParts.add(Map.of("type", "text", "text", (Object) (userMessage != null ? userMessage : "请分析这张图片的内容")));
                contentParts.add(Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)));
                messages.add(Map.of("role", "user", "content", (Object) contentParts));
            } else {
                messages.add(Map.of("role", "user", "content", (Object) userMessage));
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", true);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4096);

            log.info("调用千问: model={}, messageCount={}, hasImage={}", model, messages.size(), base64Image != null);

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("千问 API error {}: {}", response.statusCode(), errBody);
                return "AI服务请求失败(HTTP " + response.statusCode() + ")，请检查API Key和模型名称";
            }

            StringBuilder fullContent = new StringBuilder();
            int chunkCount = 0;
            int contentChunkCount = 0;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;
                        try {
                            Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                            List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                            if (choices != null && !choices.isEmpty()) {
                                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                if (delta != null) {
                                    Object contentObj = delta.get("content");
                                    if (contentObj != null) {
                                        String content = (String) contentObj;
                                        if (!content.isEmpty()) {
                                            fullContent.append(content);
                                            contentChunkCount++;
                                            if (onChunk != null) onChunk.accept(content);
                                        }
                                    }
                                }
                            }
                            chunkCount++;
                        } catch (Exception e) {
                            log.debug("SSE解析跳过一行: {}", e.getMessage());
                        }
                    }
                }
            }

            log.info("千问响应完成: totalChunks={}, contentChunks={}, contentLength={}",
                chunkCount, contentChunkCount, fullContent.length());

            if (fullContent.length() == 0 && chunkCount > 0) {
                return "AI思考时间较长但未生成有效回复，请重试或简化提问。";
            }
            if (fullContent.length() == 0) {
                return "AI未返回内容，请检查API Key是否有权限访问模型 " + model;
            }

            return fullContent.toString();

        } catch (Exception e) {
            log.error("千问 API调用异常", e);
            return "AI服务连接失败: " + e.getMessage();
        }
    }
}
