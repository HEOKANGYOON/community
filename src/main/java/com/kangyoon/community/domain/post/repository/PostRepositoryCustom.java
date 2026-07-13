package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostRepositoryCustom {
    Page<Post> findPostsByBoard(Long boardId, String keyword, String searchType, Pageable pageable);
    Page<Post> findPostsByBoardLike(Long boardId, String keyword, String searchType, Pageable pageable);
}
