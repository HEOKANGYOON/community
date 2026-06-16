package com.kangyoon.community.infrastructure.s3.dto;

public record PresignedUrlResponse(
        String presignedUrl,
        String imageUrl
) {
}
