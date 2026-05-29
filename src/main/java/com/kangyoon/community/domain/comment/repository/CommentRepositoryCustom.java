package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentRepositoryCustom {
    Page<Comment> findCommentsByPostId(Long postId, Pageable pageable);
}
