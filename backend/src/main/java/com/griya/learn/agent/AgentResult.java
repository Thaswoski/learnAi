package com.griya.learn.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    private boolean success;
    private String content;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    private String errorMessage;

    public static AgentResult ok(String content) {
        return AgentResult.builder().success(true).content(content).build();
    }

    public static AgentResult ok(String content, Map<String, Object> data) {
        return AgentResult.builder().success(true).content(content).data(data).build();
    }

    public static AgentResult fail(String errorMessage) {
        return AgentResult.builder().success(false).errorMessage(errorMessage).build();
    }
}
