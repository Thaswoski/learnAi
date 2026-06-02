package com.griya.learn.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiService {

    String tutorChat(String userMessage, String base64Image, List<Map<String, String>> history, Consumer<String> onChunk);

    String chat(String systemPrompt, String userMessage, Consumer<String> onChunk);

    String getModelName();

    default boolean supportsImage() { return false; }
}
