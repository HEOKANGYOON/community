package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;

import java.util.List;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface PostRepositoryCustom {
    List<Post> findPostsByBoard(Long boardId, Pageable pageable);
    Optional<Post> findActivePostById(Long postId);
}
