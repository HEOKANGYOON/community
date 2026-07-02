package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>, CommentLikeRepositoryCustom {
    Optional<CommentLike> findByMemberIdAndCommentId(Long memberId, Long commentId);
}
