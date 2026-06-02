package com.griya.learn.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String role;
    private String avatar;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
