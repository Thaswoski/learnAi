package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DashboardCache {
    private Long id;
    private Long userId;
    private Integer quizTotal;
    private Integer resourceTotal;
    private String dataJson;
    private LocalDateTime updatedAt;
}
