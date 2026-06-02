package com.griya.learn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名不能超过64个字符")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度需要在6-128之间")
    private String password;

    @NotBlank(message = "角色不能为空")
    private String role;
}
