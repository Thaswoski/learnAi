package com.griya.learn.community.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Post {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private Integer viewCount;
    private Integer agreeCount;
    private Integer disagreeCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
