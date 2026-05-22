package com.kangyoon.community.domain.post.dto;

import com.kangyoon.community.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long postId,
        Long boardId,
        String title,
        String memberNickname,
        int viewCount,
        int recommendationCount,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getBoard().getId(),
                post.getTitle(),
                post.getMember().getNickname(),
                post.getViewCount(),
                post.getRecommendationCount(),
                post.getCreatedAt()
        );
    }
}
