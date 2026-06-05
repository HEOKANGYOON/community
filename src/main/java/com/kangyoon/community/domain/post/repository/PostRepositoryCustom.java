package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface PostRepositoryCustom {
    Page<Post> findPostsByBoard(Long boardId, String keyword, String searchType, Pageable pageable);
    Optional<Post> findActivePostById(Long postId);
}
