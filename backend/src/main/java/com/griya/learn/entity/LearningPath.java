package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LearningPath {
    private Long id;
    private Long userId;
    private Integer quizTotal;
    private String dataJson;
    private LocalDateTime updatedAt;
}
