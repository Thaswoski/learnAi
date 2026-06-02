package com.griya.learn.community.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PostAttitude {
    private Long id;
    private Long postId;
    private Long userId;
    private Integer attitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
