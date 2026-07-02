package com.kangyoon.community.domain.comment.dto;

import com.kangyoon.community.domain.comment.entity.Comment;

import java.time.LocalDateTime;

public record CommentDetailResponse(
        Long commentId,
        Long postId,
        Long memberId,
        Long parentId,
        String content,
        int likeCount,
        LocalDateTime createdAt
) {
    public static CommentDetailResponse from(Comment comment) {
        return new CommentDetailResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getMember().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                comment.getLikeCount(),
                comment.getCreatedAt()
        );
    }
}
