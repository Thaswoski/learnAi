package com.griya.learn.community.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Article {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String category;
    private Long authorId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;
    private Integer isTop;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
