package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    Optional<Post> findByIdAndDeletedAtIsNull(Long id);
    Page<Post> findByBoardIdAndDeletedAtIsNull(Long boardId, Pageable pageable);
}
