package com.griya.learn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service("sparkAiService")
public class SparkAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(SparkAiService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spark.app-id:}")
    private String appId;

    @Value("${spark.api-key:}")
    private String apiKey;

    @Value("${spark.api-secret:}")
    private String apiSecret;

    @Value("${spark.host-url:wss://spark-api.xf-yun.com/x2}")
    private String hostUrl;

    @Value("${spark.domain:spark-x}")
    private String domain;

    private static final String TUTOR_SYSTEM_PROMPT =
        "你是一个专业的AI智能辅导老师，名为智多星。你的任务是帮助学生解决学习中的各种问题，包括但不限于：" +
        "1. 解答课程知识点的疑问 2. 讲解算法和编程概念 3. 辅导作业和习题 " +
        "4. 提供学习方法建议 5. 分析错题原因 6. 推荐学习资源。" +
        "请用亲切、耐心的语气回复，像一位真正的老师一样引导学生思考，而不是直接给答案。" +
        "回答时适当使用Markdown格式让内容更清晰（标题、列表、代码块等），但不要过于冗长。" +
        "如果学生问的问题你不确定，请诚实地说明并提供进一步学习的建议。";

    public String getModelName() { return "讯飞星火 (X2)"; }

    @Override
    public String tutorChat(String userMessage, String base64Image, List<Map<String, String>> history, Consumer<String> onChunk) {
        if (appId == null || appId.isEmpty() || "your_app_id".equals(appId)) {
            return "讯飞星火未配置，请在application.yml中设置 spark.app-id, spark.api-key, spark.api-secret";
        }
        String msg = userMessage;
        if (base64Image != null && !base64Image.isEmpty()) {
            msg = "用户上传了一张图片，" + userMessage;
        }
        return doChat(TUTOR_SYSTEM_PROMPT, msg, onChunk);
    }

    @Override
    public String chat(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        return doChat(systemPrompt, userMessage, onChunk);
    }

    private String doChat(String systemPrompt, String userMessage, Consumer<String> onChunk) {

        try {
            URI uri = new URI(hostUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) path = "/";

            String rfc1123Date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));

            String signatureOrigin = "host: " + host + "\n" + "date: " + rfc1123Date + "\n" + "GET " + path + " HTTP/1.1";
            log.info("星火签名原文: {}", signatureOrigin.replace("\n", "\\n"));

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawSignature = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(rawSignature);

            String authOrigin = "api_key=\"" + apiKey + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"" + signature + "\"";
            String authorization = Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));

            log.info("星火鉴权: date={}, signature={}, authLen={}", rfc1123Date, signature, authorization.length());

            String wsUrl = hostUrl
                + "?authorization=" + URLEncoder.encode(authorization, "UTF-8")
                + "&date=" + URLEncoder.encode(rfc1123Date, "UTF-8")
                + "&host=" + URLEncoder.encode(host, "UTF-8");

            log.info("星火连接: appId={}, host={}, path={}", appId, host, path);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            StringBuilder fullContent = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            WebSocketClient wsClient = new WebSocketClient(URI.create(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("星火WS连接成功: status={}", handshake.getHttpStatus());
                    try {
                        Map<String, Object> frame = buildRequestFrame(messages);
                        String json = objectMapper.writeValueAsString(frame);
                        log.info("星火发送请求: {} chars", json.length());
                        send(json);
                    } catch (Exception e) {
                        log.error("星火发送失败", e);
                        fullContent.append("星火请求构建失败: ").append(e.getMessage());
                        close();
                        latch.countDown();
                    }
                }

                @Override
                public void onMessage(String text) {
                    log.debug("星火收到: {} chars", text.length());
                    try {
                        Map<String, Object> resp = objectMapper.readValue(text, Map.class);
                        Map<String, Object> header = (Map<String, Object>) resp.get("header");
                        int code = ((Number) header.getOrDefault("code", -1)).intValue();
                        if (code != 0) {
                            log.error("星火业务错误 code={}: {}", code, text);
                            fullContent.append("星火API错误(code=" + code + "): " + header.getOrDefault("message", ""));
                            close();
                            latch.countDown();
                            return;
                        }

                        Map<String, Object> payload = (Map<String, Object>) resp.get("payload");
                        if (payload != null) {
                            Map<String, Object> choices = (Map<String, Object>) payload.get("choices");
                            if (choices != null) {
                                List<Map<String, Object>> textList = (List<Map<String, Object>>) choices.get("text");
                                if (textList != null && !textList.isEmpty()) {
                                    String content = (String) textList.get(0).get("content");
                                    if (content != null && !content.isEmpty()) {
                                        fullContent.append(content);
                                        if (onChunk != null) onChunk.accept(content);
                                    }
                                }
                                int status = ((Number) choices.getOrDefault("status", 0)).intValue();
                                if (status == 2) {
                                    close();
                                    latch.countDown();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.debug("星火解析跳帧");
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("星火WS关闭: code={}, reason={}, remote={}", code, reason, remote);
                    latch.countDown();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("星火WS异常: {}", ex.getMessage());
                    fullContent.append("讯飞星火连接失败: ").append(ex.getMessage())
                        .append("。请检查 spark.app-id / spark.api-key / spark.api-secret 是否正确。");
                    latch.countDown();
                }
            };

            wsClient.connectBlocking(10, TimeUnit.SECONDS);
            latch.await(60, TimeUnit.SECONDS);

            log.info("星火响应完成: contentLength={}", fullContent.length());

            if (fullContent.length() == 0) return "AI未返回内容，请检查星火API配置";
            return fullContent.toString();

        } catch (Exception e) {
            log.error("星火调用异常", e);
            return "讯飞星火服务连接失败: " + e.getMessage();
        }
    }

    private Map<String, Object> buildRequestFrame(List<Map<String, String>> messages) {
        Map<String, Object> frame = new HashMap<>();

        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);
        frame.put("header", header);

        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        chat.put("domain", domain);
        chat.put("temperature", 0.7);
        chat.put("max_tokens", 4096);
        parameter.put("chat", chat);
        frame.put("parameter", parameter);

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        List<Map<String, String>> text = new ArrayList<>();
        for (Map<String, String> m : messages) {
            text.add(Map.of("role", m.get("role"), "content", m.get("content")));
        }
        message.put("text", text);
        payload.put("message", message);
        frame.put("payload", payload);

        return frame;
    }
}
