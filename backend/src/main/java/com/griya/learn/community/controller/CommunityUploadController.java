package com.griya.learn.community.controller;

import com.griya.learn.config.WebResourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

@RestController
@RequestMapping("/api/community/upload")
public class CommunityUploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Map.of("code", 500, "message", "文件不能为空");
        }
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String fileName = "community_" + UUID.randomUUID().toString().replace("-", "") + ext;

            File dir = new File(WebResourceConfig.resolveDir(uploadDir), "community");
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(dir, fileName);
            Files.write(dest.toPath(), file.getBytes());

            String url = "/uploads/community/" + fileName;
            return Map.of("code", 200, "data", Map.of("url", url, "name", originalName));
        } catch (Exception e) {
            return Map.of("code", 500, "message", "上传失败: " + e.getMessage());
        }
    }
}
