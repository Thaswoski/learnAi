package com.griya.learn.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String email;
    private String avatar;
}
