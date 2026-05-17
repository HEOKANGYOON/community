package com.kangyoon.community.domain.board.dto;

import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String categoryName,
        String name,
        String description,
        LocalDateTime createdAt
) { }
