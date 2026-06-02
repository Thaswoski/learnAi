package com.griya.learn.community.controller;

import com.griya.learn.community.entity.Comment;
import com.griya.learn.community.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{postId}")
    public Map<String, Object> getCommentsByPostId(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        return Map.of("code", 200, "data", comments);
    }

    @PostMapping
    public Map<String, Object> createComment(@RequestBody Comment comment) {
        commentService.createComment(comment);
        return Map.of("code", 200, "message", "评论成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Map.of("code", 200, "message", "删除成功");
    }
}
