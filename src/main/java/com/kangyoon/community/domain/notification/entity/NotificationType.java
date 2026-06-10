package com.kangyoon.community.domain.notification.entity;

public enum NotificationType {
    COMMENT,
    REPLY,
    COMMENT_LIKE;

    public String toContent() {
        return switch (this) {
            case COMMENT -> "게시글에 새 댓글이 달렸습니다.";
            case REPLY -> "내 댓글에 대댓글이 달렸습니다.";
            case COMMENT_LIKE -> "게시글 작성자가 내 댓글을 좋아합니다.";
        };
    }
}
