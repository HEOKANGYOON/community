package com.kangyoon.community.domain.comment.dto;

import com.kangyoon.community.domain.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommentsResponse {
    private Long id;
    private Long memberId;
    private Long parentId;
    private String content;
    private int commentLikeCount;
    private LocalDateTime createdAt;


    public static CommentsResponse from(Comment comment, int commentLikeCount) {
        CommentsResponse dto = new CommentsResponse();
        if (comment.getDeletedAt() != null) {
            dto.id = comment.getId();
            dto.content = "삭제된 댓글입니다.";
            dto.createdAt = comment.getCreatedAt();
            dto.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
            return dto;
        }

        dto.id = comment.getId();
        dto.memberId = comment.getMember().getId();
        dto.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        dto.commentLikeCount = commentLikeCount;
        dto.content = comment.getContent();
        dto.createdAt = comment.getCreatedAt();
        return dto;
    }

}
