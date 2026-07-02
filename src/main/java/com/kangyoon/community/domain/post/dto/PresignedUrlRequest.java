package com.kangyoon.community.domain.post.dto;

public record PresignedUrlRequest(
        String fileName,
        String contentType
) { }
