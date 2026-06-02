package com.griya.learn.agent;

import com.griya.learn.entity.StudentProfile;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class AgentContext {
    private Long userId;
    private String courseName;
    private String major;
    private String knowledgeGaps;
    private String learningNeeds;
    private String resourceType;

    @Builder.Default
    private Map<String, Object> sharedData = new ConcurrentHashMap<>();

    private StudentProfile profile;
    private String userMessage;

    private AgentEventBus eventBus;

    public void put(String key, Object value) {
        sharedData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) sharedData.get(key);
    }

    public <T> T get(String key, T defaultValue) {
        T val = get(key);
        return val != null ? val : defaultValue;
    }

    public boolean has(String key) {
        return sharedData.containsKey(key);
    }

    public AgentEvent emit(AgentEvent.Type type, String sourceAgent) {
        if (eventBus != null) {
            return eventBus.emit(AgentEvent.of(type, sourceAgent));
        }
        return null;
    }

    public AgentEvent emit(AgentEvent.Type type, String sourceAgent, Map<String, Object> payload) {
        if (eventBus != null) {
            return eventBus.emit(AgentEvent.of(type, sourceAgent, payload));
        }
        return null;
    }

    public static AgentContext fromUserInput(Long userId, String courseName, String major,
                                              String knowledgeGaps, String learningNeeds,
                                              String resourceType) {
        return AgentContext.builder()
                .userId(userId)
                .courseName(courseName)
                .major(major)
                .knowledgeGaps(knowledgeGaps)
                .learningNeeds(learningNeeds)
                .resourceType(resourceType)
                .build();
    }
}
