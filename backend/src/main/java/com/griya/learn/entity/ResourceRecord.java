package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ResourceRecord {
    private Long id;
    private Long userId;
    private String courseName;
    private String knowledgePoint;
    private String resourceType;
    private String imageUrl;
    private String fileName;
    private LocalDateTime createdAt;
}
