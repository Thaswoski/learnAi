package com.griya.learn.community.service;

import com.griya.learn.community.entity.Comment;
import com.griya.learn.community.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public List<Comment> getCommentsByPostId(Long postId) {
        List<Comment> all = commentMapper.selectByPostId(postId);
        return buildCommentTree(all);
    }

    public Comment getById(Long id) {
        return commentMapper.selectById(id);
    }

    @Transactional
    public void createComment(Comment comment) {
        if (comment.getParentId() != null && comment.getParentId() != 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                if (comment.getToUserId() == null) comment.setToUserId(parent.getAuthorId());
                if (comment.getToUsername() == null) comment.setToUsername(parent.getAuthorName());
            }
        }
        commentMapper.insert(comment);
    }

    @Transactional
    public void deleteComment(Long id) {
        commentMapper.updateStatus(id, 0);
    }

    private List<Comment> buildCommentTree(List<Comment> all) {
        Map<Long, Comment> map = new HashMap<>();
        List<Comment> top = new ArrayList<>();
        for (Comment c : all) {
            map.put(c.getId(), c);
            c.setReplies(new ArrayList<>());
            if (c.getParentId() == null || c.getParentId() == 0) {
                top.add(c);
            }
        }
        for (Comment c : all) {
            if (c.getParentId() != null && c.getParentId() != 0) {
                Comment parent = map.get(c.getParentId());
                if (parent != null) {
                    if (c.getToUsername() == null) c.setToUsername(parent.getAuthorName());
                    parent.getReplies().add(c);
                }
            }
        }
        return top;
    }
}
