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

@Service("deepseekAiService")
public class DeepSeekService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private static final String PROFILE_SYSTEM_PROMPT =
        "你是一个专业的学生画像分析师。请用自然轻松的对话方式了解学生的以下9个维度：" +
        "1.学习目标 2.知识点掌握 3.整体水平 4.学习节奏 " +
        "5.认知偏好 6.易错类型 7.资源偏好 8.反馈偏好 9.综合总结。" +
        "每次只聚焦1-2个维度提问，不要一次性问太多。回答简洁亲切，控制在200字以内。" +
        "对话足够深入后，请在最后一条消息末尾附加一段JSON：\n" +
        "<PROFILE_JSON>\n{\"profile\": {\"learningGoal\":{\"purpose\":\"\",\"weeklyHours\":\"\"},\"knowledgeMastery\":[{\"name\":\"\",\"status\":\"\",\"score\":0}],\"overallLevel\":\"\",\"studyRhythm\":{\"studySlot\":\"\",\"focusDuration\":\"\",\"habit\":\"\"},\"cognitiveStyle\":{\"mediaPreference\":[],\"understanding\":\"\"},\"errorPattern\":[{\"type\":\"\",\"frequency\":\"\",\"cause\":\"\"}],\"resourcePreference\":{\"difficulty\":\"\",\"contentLength\":\"\",\"acceptExtension\":true},\"feedbackPreference\":{\"answerStyle\":\"\",\"feedbackFrequency\":\"\"},\"diagnosisReport\":\"\"}}\n</PROFILE_JSON>";

    public String chat(String userMessage, List<Map<String, String>> history, Consumer<String> onChunk) {
        return chat(PROFILE_SYSTEM_PROMPT, userMessage, history, onChunk);
    }

    @Override
    public String chat(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        return chat(systemPrompt, userMessage, null, onChunk);
    }

    @Override
    public String tutorChat(String userMessage, String base64Image, List<Map<String, String>> history, Consumer<String> onChunk) {
        String prompt = userMessage;
        if (base64Image != null && !base64Image.isEmpty()) {
            prompt = "用户上传了一张图片，" + userMessage;
        }
        return chat(prompt, history, onChunk);
    }

    @Override
    public String getModelName() { return "DeepSeek (" + model + ")"; }

    public String chat(String systemPrompt, String userMessage, List<Map<String, String>> history, Consumer<String> onChunk) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null) {
                messages.addAll(history);
            }

            messages.add(Map.of("role", "user", "content", userMessage));

            return callApi(messages, onChunk);
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            return "AI服务连接失败: " + e.getMessage();
        }
    }

    private String callApi(List<Map<String, String>> messages, Consumer<String> onChunk) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 4096);

        log.info("调用DeepSeek: model={}, messageCount={}", model, messages.size());

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
            log.error("DeepSeek API error {}: {}", response.statusCode(), errBody);
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

        log.info("DeepSeek响应完成: totalChunks={}, contentChunks={}, contentLength={}",
            chunkCount, contentChunkCount, fullContent.length());

        if (fullContent.length() == 0 && chunkCount > 0) {
            return "AI思考时间较长但未生成有效回复，请重试或简化提问。";
        }
        if (fullContent.length() == 0) {
            return "AI未返回内容，请检查API Key是否有权限访问模型 " + model;
        }

        return fullContent.toString();
    }
}
