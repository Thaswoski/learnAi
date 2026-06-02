package com.griya.learn.community.service;

import com.griya.learn.community.entity.Article;
import com.griya.learn.community.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;

    public Map<String, Object> getPublishedList(Integer page, Integer size, String category) {
        int offset = (page - 1) * size;
        List<Article> articles = articleMapper.selectPage(offset, size, category);
        int total = articleMapper.count(category);
        Map<String, Object> result = new HashMap<>();
        result.put("records", articles);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return result;
    }

    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    @Transactional
    public void createArticle(Article article) {
        articleMapper.insert(article);
    }

    @Transactional
    public void updateArticle(Article article) {
        articleMapper.updateById(article);
    }

    public void incrementViewCount(Long id) {
        articleMapper.incrementViewCount(id);
    }

    public void incrementLikeCount(Long id) {
        articleMapper.incrementLikeCount(id);
    }
}
