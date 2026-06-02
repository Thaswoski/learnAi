package com.griya.learn.community.controller;

import com.griya.learn.community.entity.Article;
import com.griya.learn.community.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) String category) {
        return articleService.getPublishedList(page, size, category);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        articleService.incrementViewCount(id);
        Article article = articleService.getById(id);
        return Map.of("code", 200, "data", article != null ? article : Map.of());
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Article article) {
        articleService.createArticle(article);
        return Map.of("code", 200, "message", "创建成功");
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Article article) {
        article.setId(id);
        articleService.updateArticle(article);
        return Map.of("code", 200, "message", "更新成功");
    }

    @PostMapping("/{id}/like")
    public Map<String, Object> like(@PathVariable Long id) {
        articleService.incrementLikeCount(id);
        return Map.of("code", 200, "message", "点赞成功");
    }

    @GetMapping("/categories")
    public Map<String, Object> categories() {
        List<Map<String, Object>> cats = Arrays.asList(
            Map.of("id", 1, "name", "非遗文化"),
            Map.of("id", 2, "name", "传统工艺"),
            Map.of("id", 3, "name", "民间艺术"),
            Map.of("id", 4, "name", "历史传承"),
            Map.of("id", 5, "name", "文化研究")
        );
        return Map.of("code", 200, "data", cats);
    }
}
