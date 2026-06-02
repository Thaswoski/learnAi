package com.griya.learn.community.controller;

import com.griya.learn.community.entity.Post;
import com.griya.learn.community.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public Map<String, Object> getPostList(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size) {
        Map<String, Object> data = postService.getPostList(page, size);
        return Map.of("code", 200, "data", data);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPostDetail(@PathVariable Long id) {
        Post post = postService.getPostDetail(id);
        return Map.of("code", 200, "data", post != null ? post : Map.of());
    }

    @PostMapping
    public Map<String, Object> createPost(@RequestBody Post post) {
        postService.createPost(post);
        return Map.of("code", 200, "message", "发帖成功");
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePost(@PathVariable Long id, @RequestBody Post post) {
        post.setId(id);
        postService.updatePost(post);
        return Map.of("code", 200, "message", "更新成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Map.of("code", 200, "message", "删除成功");
    }

    @PostMapping("/attitude")
    public Map<String, Object> attitude(@RequestBody Map<String, Object> request) {
        Long postId = ((Number) request.get("postId")).longValue();
        Long userId = ((Number) request.get("userId")).longValue();
        Integer attitude = (Integer) request.get("attitude");
        Map<String, Object> result = postService.updatePostAttitude(postId, userId, attitude);
        return Map.of("code", 200, "data", result);
    }
}
