package com.kangyoon.community.domain.board.dto;

import com.kangyoon.community.domain.board.entity.Board;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String categoryName,
        String name,
        String description,
        LocalDateTime createdAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getBoardCategory().getName(),
                board.getName(),
                board.getDescription(),
                board.getCreatedAt()
        );
    }
}
