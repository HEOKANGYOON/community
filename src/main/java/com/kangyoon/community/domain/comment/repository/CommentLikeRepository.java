package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>, CommentLikeRepositoryCustom {
    Optional<CommentLike> findByMemberIdAndCommentId(Long memberId, Long commentId);

    @Query("SELECT DISTINCT cl.comment.id FROM CommentLike cl WHERE cl.createdAt >= :since")
    List<Long> findDistinctCommentIdsSince(@Param("since") LocalDateTime since);
}
