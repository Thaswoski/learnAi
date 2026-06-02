package com.griya.learn.controller;

import com.griya.learn.common.Result;
import com.griya.learn.config.WebResourceConfig;
import com.griya.learn.dto.ChangePasswordRequest;
import com.griya.learn.dto.LoginResponse;
import com.griya.learn.dto.UpdateProfileRequest;
import com.griya.learn.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/profile")
    public Result<LoginResponse> getProfile(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        return Result.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public Result<LoginResponse> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid UpdateProfileRequest req) {
        Long userId = userService.getUserByToken(token).getId();
        LoginResponse resp = userService.updateProfile(userId, req);
        return Result.success("个人信息更新成功", resp);
    }

    @PostMapping("/avatar")
    public Result<String> uploadAvatar(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {
        try {
            Long userId = userService.getUserByToken(token).getId();
            if (file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

            File dir = new File(WebResourceConfig.resolveDir(uploadDir), "avatars");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(dir, fileName);
            file.transferTo(dest);

            String avatarUrl = "/uploads/avatars/" + fileName;
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setAvatar(avatarUrl);
            userService.updateProfile(userId, req);

            log.info("头像上传成功: {}", avatarUrl);
            return Result.success("头像上传成功", avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error(500, "头像上传失败: " + e.getMessage());
        }
    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid ChangePasswordRequest req) {
        Long userId = userService.getUserByToken(token).getId();
        userService.changePassword(userId, req);
        return Result.success("密码修改成功", null);
    }
}
