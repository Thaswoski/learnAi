package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CQuestion {
    private Integer id;
    private String title;
    private String problem;
    private String difficulty;
    private String knowledgePoint;
    private String inputExample;
    private String outputExample;
    private String codeTemplate;
    private String answerHint;
    private LocalDateTime createdAt;
}
