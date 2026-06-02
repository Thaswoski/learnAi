package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private Long userId;
    private String sessionId;
    private String role;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
}
