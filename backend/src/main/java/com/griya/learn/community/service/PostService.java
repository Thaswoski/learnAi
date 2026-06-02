package com.griya.learn.community.service;

import com.griya.learn.community.entity.Post;
import com.griya.learn.community.entity.PostAttitude;
import com.griya.learn.community.mapper.PostAttitudeMapper;
import com.griya.learn.community.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final PostAttitudeMapper postAttitudeMapper;

    public Map<String, Object> getPostList(Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Post> posts = postMapper.selectPage(offset, size);
        int total = postMapper.count();
        Map<String, Object> result = new HashMap<>();
        result.put("records", posts);
        result.put("total", total);
        result.put("current", page);
        result.put("size", size);
        return result;
    }

    public Post getPostDetail(Long id) {
        Post post = postMapper.selectById(id);
        if (post != null) {
            postMapper.incrementViewCount(id);
        }
        return post;
    }

    @Transactional
    public void createPost(Post post) {
        postMapper.insert(post);
    }

    @Transactional
    public void updatePost(Post post) {
        postMapper.updateById(post);
    }

    @Transactional
    public void deletePost(Long id) {
        postMapper.deleteById(id);
    }

    @Transactional
    public Map<String, Object> updatePostAttitude(Long postId, Long userId, Integer attitude) {
        Map<String, Object> result = new HashMap<>();
        PostAttitude existing = postAttitudeMapper.selectByPostAndUser(postId, userId);

        int agreeDelta = 0;
        int disagreeDelta = 0;
        int userAttitude = attitude;

        if (existing != null) {
            int oldAttitude = existing.getAttitude();
            if (oldAttitude == attitude) {
                userAttitude = 0;
                postAttitudeMapper.deleteById(existing.getId());
                if (oldAttitude == 1) agreeDelta = -1;
                else if (oldAttitude == -1) disagreeDelta = -1;
            } else {
                existing.setAttitude(attitude);
                postAttitudeMapper.updateById(existing);
                if (oldAttitude == 1) agreeDelta = -1;
                else if (oldAttitude == -1) disagreeDelta = -1;
                if (attitude == 1) agreeDelta += 1;
                else if (attitude == -1) disagreeDelta += 1;
            }
        } else {
            PostAttitude pa = new PostAttitude();
            pa.setPostId(postId);
            pa.setUserId(userId);
            pa.setAttitude(attitude);
            postAttitudeMapper.insert(pa);
            if (attitude == 1) agreeDelta = 1;
            else if (attitude == -1) disagreeDelta = 1;
        }

        if (agreeDelta != 0) postMapper.updateAgreeCount(postId, agreeDelta);
        if (disagreeDelta != 0) postMapper.updateDisagreeCount(postId, disagreeDelta);

        result.put("userAttitude", userAttitude);
        return result;
    }
}
