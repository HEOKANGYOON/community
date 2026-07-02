package com.kangyoon.community.domain.board.dto;

import jakarta.validation.constraints.Size;

public record BoardCreateResponse (
        String name,
        @Size(max = 200) String description,
        Long categoryId
){ }
