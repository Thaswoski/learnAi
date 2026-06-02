package com.griya.learn.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final List<TaskStep> steps = new ArrayList<>();
    private Consumer<String> onProgress;

    public WorkflowEngine onProgress(Consumer<String> onProgress) {
        this.onProgress = onProgress;
        return this;
    }

    public WorkflowEngine addStep(TaskStep step) {
        steps.add(step);
        return this;
    }

    public void execute() {
        StringBuilder allResults = new StringBuilder();

        for (TaskStep step : steps) {
            step.setStatus(TaskStatus.RUNNING);
            emitProgress(step, null);

            try {
                AgentResult result = step.getTask().get();
                step.setResult(result);
                step.setStatus(result.isSuccess() ? TaskStatus.COMPLETED : TaskStatus.FAILED);

                if (result.isSuccess() && result.getContent() != null) {
                    allResults.append(result.getContent()).append("\n");
                }

                emitProgress(step, result);

                if (!result.isSuccess()) {
                    log.warn("[Workflow] 步骤 {} 失败: {}", step.getId(), result.getErrorMessage());
                    break;
                }
            } catch (Exception e) {
                log.error("[Workflow] 步骤 {} 异常: {}", step.getId(), e.getMessage(), e);
                step.setStatus(TaskStatus.FAILED);
                step.setResult(AgentResult.fail(e.getMessage()));
                emitProgress(step, step.getResult());
                break;
            }
        }
    }

    private void emitProgress(TaskStep step, AgentResult result) {
        if (onProgress == null) return;
        try {
            java.util.Map<String, Object> event = new java.util.LinkedHashMap<>();
            event.put("type", "agent_step");
            event.put("stepId", step.getId());
            event.put("agent", step.getAgentName());
            event.put("description", step.getDescription());
            event.put("icon", step.getIcon());
            event.put("status", step.getStatus().name().toLowerCase());
            if (result != null) {
                event.put("success", result.isSuccess());
                if (result.getErrorMessage() != null) {
                    event.put("error", result.getErrorMessage());
                }
            }
            onProgress.accept(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.debug("进度推送序列化失败: {}", e.getMessage());
        }
    }

    public boolean isAllSuccess() {
        return steps.stream().allMatch(s -> s.getStatus() == TaskStatus.COMPLETED);
    }

    public TaskStep getLastFailed() {
        return steps.stream()
                .filter(s -> s.getStatus() == TaskStatus.FAILED)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    public List<TaskStep> getSteps() {
        return steps;
    }
}
