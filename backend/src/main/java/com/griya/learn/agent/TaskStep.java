package com.griya.learn.agent;

import lombok.Builder;
import lombok.Data;

import java.util.function.Supplier;

@Data
@Builder
public class TaskStep {
    private String id;
    private String agentName;
    private String description;
    private String icon;

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private Supplier<AgentResult> task;
    private AgentResult result;

    public static TaskStep of(String id, String agentName, String description, String icon,
                               Supplier<AgentResult> task) {
        return TaskStep.builder()
                .id(id)
                .agentName(agentName)
                .description(description)
                .icon(icon)
                .task(task)
                .build();
    }

    public MapBuilder toMap() {
        return new MapBuilder(this);
    }

    public static class MapBuilder {
        private final TaskStep step;

        MapBuilder(TaskStep step) {
            this.step = step;
        }

        public java.util.Map<String, Object> build() {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("step", step.id);
            map.put("agent", step.agentName);
            map.put("message", step.description);
            map.put("icon", step.icon);
            map.put("status", step.status.name().toLowerCase());
            if (step.result != null) {
                map.put("success", step.result.isSuccess());
                if (!step.result.isSuccess() && step.result.getErrorMessage() != null) {
                    map.put("error", step.result.getErrorMessage());
                }
            }
            return map;
        }
    }
}
