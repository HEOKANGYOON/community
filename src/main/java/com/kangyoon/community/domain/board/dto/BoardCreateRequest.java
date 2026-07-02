package com.kangyoon.community.domain.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardCreateRequest(
        @NotBlank (message = "게시판 이름은 필수입니다.") String name,
        @NotBlank (message = "게시판 설명은 필수입니다.") @Size(max = 200) String description,
        @NotNull Long categoryId
) { }
