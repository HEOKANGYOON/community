package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, CommentRepositoryCustom {
    Optional<Comment> findByIdAndDeletedAtIsNull(Long postId);
}
