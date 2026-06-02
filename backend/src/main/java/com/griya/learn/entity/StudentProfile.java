package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentProfile {
    private Long id;
    private Long userId;
    private String knowledgeMastery;
    private String overallLevel;
    private String diagnosisReport;
    private String studyRhythm;
    private String cognitiveStyle;
    private String learningGoal;
    private String errorPattern;
    private String resourcePreference;
    private String feedbackPreference;
    private Integer completedDimensions;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
