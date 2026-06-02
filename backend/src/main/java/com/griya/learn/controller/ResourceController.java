package com.griya.learn.controller;

import com.griya.learn.entity.ResourceRecord;
import com.griya.learn.mapper.ResourceRecordMapper;
import com.griya.learn.service.MindMapService;
import com.griya.learn.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final MindMapService mindMapService;
    private final UserService userService;
    private final ResourceRecordMapper resourceRecordMapper;

    public ResourceController(MindMapService mindMapService,
                               UserService userService, ResourceRecordMapper resourceRecordMapper) {
        this.mindMapService = mindMapService;
        this.userService = userService;
        this.resourceRecordMapper = resourceRecordMapper;
    }

    @PostMapping("/mindmap")
    public Map<String, Object> generateMindMap(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        Long userId = userService.getUserByToken(token).getId();
        String major = body.getOrDefault("major", "");
        String courseName = body.getOrDefault("courseName", "");
        String knowledgeGaps = body.getOrDefault("knowledgeGaps", "");
        String learningNeeds = body.getOrDefault("learningNeeds", "");
        String model = body.getOrDefault("model", "deepseek");
        String searchModel = body.getOrDefault("searchModel", "xfsearch");

        if (courseName.isBlank()) {
            return Map.of("code", 400, "message", "课程内容不能为空");
        }

        return mindMapService.generateMindMap(userId, major, courseName, knowledgeGaps, learningNeeds, model, searchModel);
    }

    @GetMapping("/history")
    public Map<String, Object> getHistory(@RequestHeader("Authorization") String token) {
        Long userId = userService.getUserByToken(token).getId();
        List<ResourceRecord> records = resourceRecordMapper.selectByUser(userId);
        records.removeIf(r -> "video".equals(r.getResourceType()));
        return Map.of("code", 200, "data", records);
    }

    @PostMapping("/record")
    public Map<String, Object> saveRecord(
            @RequestHeader("Authorization") String token,
            @RequestBody ResourceRecord record) {
        if ("video".equals(record.getResourceType())) {
            return Map.of("code", 200, "message", "视频生成不记录");
        }
        Long userId = userService.getUserByToken(token).getId();
        record.setUserId(userId);
        resourceRecordMapper.insert(record);
        return Map.of("code", 200, "message", "记录已保存");
    }

    @GetMapping(value = "/mindmap-image/{fileName}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] getMindMapImage(@PathVariable String fileName) {
        try {
            File file = Paths.get("downloadData/xmind", fileName).toFile();
            if (!file.exists()) return new byte[0];
            try (FileInputStream fis = new FileInputStream(file)) {
                return fis.readAllBytes();
            }
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @GetMapping("/download/{type}/{fileName}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String type,
            @PathVariable String fileName) {
        try {
            String[] allowedTypes = {"ppt", "knowledge", "exercise", "exploreReading"};
            boolean valid = false;
            for (String t : allowedTypes) {
                if (t.equals(type)) { valid = true; break; }
            }
            if (!valid) return ResponseEntity.badRequest().build();

            File file = Paths.get("downloadData/" + type, fileName).toFile();
            if (!file.exists()) return ResponseEntity.notFound().build();

            byte[] data = Files.readAllBytes(file.toPath());

            MediaType mediaType;
            if (fileName.endsWith(".pptx")) {
                mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            } else if (fileName.endsWith(".docx")) {
                mediaType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

            return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + encodedFileName)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
