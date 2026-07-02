package com.kangyoon.community.domain.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "제목 입력은 필수입니다.") @Size(max = 200) String title,
        @NotNull(message = "내용 입력은 필수입니다.") String content
) { }
