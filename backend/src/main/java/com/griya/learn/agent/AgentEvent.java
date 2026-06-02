package com.griya.learn.agent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AgentEvent {

    public enum Type {
        SEARCH_COMPLETED,
        SEARCH_INSUFFICIENT,
        CONTENT_GENERATED,
        REVIEW_COMPLETED,
        REVIEW_LOW_SCORE,
        REVIEW_PASSED,
        PLAN_ADJUST_NEEDED,
        RETRY_REQUEST,
        WORKFLOW_DONE,
        WORKFLOW_ERROR
    }

    private final String eventId;
    private final Type type;
    private final String sourceAgent;
    private final Map<String, Object> payload;
    private final long timestamp;

    private AgentEvent(Type type, String sourceAgent, Map<String, Object> payload) {
        this.eventId = UUID.randomUUID().toString().substring(0, 8);
        this.type = type;
        this.sourceAgent = sourceAgent;
        this.payload = payload != null ? new LinkedHashMap<>(payload) : new LinkedHashMap<>();
        this.timestamp = Instant.now().toEpochMilli();
    }

    public static AgentEvent of(Type type, String sourceAgent) {
        return new AgentEvent(type, sourceAgent, null);
    }

    public static AgentEvent of(Type type, String sourceAgent, Map<String, Object> payload) {
        return new AgentEvent(type, sourceAgent, payload);
    }

    public AgentEvent with(String key, Object value) {
        this.payload.put(key, value);
        return this;
    }

    public String eventId() { return eventId; }
    public Type type() { return type; }
    public String sourceAgent() { return sourceAgent; }
    public Map<String, Object> payload() { return payload; }
    public long timestamp() { return timestamp; }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) payload.get(key);
    }

    public <T> T get(String key, T defaultVal) {
        T val = get(key);
        return val != null ? val : defaultVal;
    }

    @Override
    public String toString() {
        return "AgentEvent{" + type + " from=" + sourceAgent + " id=" + eventId + " " + payload + "}";
    }
}
