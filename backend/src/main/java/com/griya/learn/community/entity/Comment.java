package com.griya.learn.community.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Comment {
    private Long id;
    private Long postId;
    private Long heritageId;
    private Long parentId;
    private Long authorId;
    private String authorName;
    private Long toUserId;
    private String toUsername;
    private String content;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Comment> replies;
}
