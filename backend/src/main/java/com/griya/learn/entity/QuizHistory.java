package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuizHistory {
    private Long id;
    private Long userId;
    private Integer questionId;
    private String questionTitle;
    private String userCode;
    private String result;
    private String expectedOutput;
    private String actualOutput;
    private String errorMessage;
    private LocalDateTime createdAt;
}
