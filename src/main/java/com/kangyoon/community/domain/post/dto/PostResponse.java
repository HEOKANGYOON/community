package com.kangyoon.community.domain.post.dto;

import com.kangyoon.community.domain.post.entity.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long postId,
        Long boardId,
        String title,
        String content,
        String memberNickname,
        int viewCount,
        int recommendationCount,
        int disrecommendationCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post, int viewCount, int recommendationCount, int disrecommendationCount) {
        return new PostResponse(
                post.getId(),
                post.getBoard().getId(),
                post.getTitle(),
                post.getContent(),
                post.getMember().getNickname(),
                viewCount,
                recommendationCount,
                disrecommendationCount,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
