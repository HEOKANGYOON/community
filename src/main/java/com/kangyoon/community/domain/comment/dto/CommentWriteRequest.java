package com.kangyoon.community.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentWriteRequest(
        @NotBlank @Size(max = 500) String content
) {
}
