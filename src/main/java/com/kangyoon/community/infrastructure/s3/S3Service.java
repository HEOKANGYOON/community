package com.kangyoon.community.infrastructure.s3;

import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.infrastructure.s3.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public PresignedUrlResponse generatePresignedUrl(String originalFilename, String contentType) {
        validateContentType(contentType);

        String key = generateKey(originalFilename);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)     //버켓 지정
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String presignedUrl = presignedRequest.url().toString();
        String imageUrl = buildImageUrl(key);

        return new PresignedUrlResponse(presignedUrl, imageUrl);
    }

    //지원하는 확장자인지 검증
    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new CustomException(ErrorCode.FORMAT_NOT_SUPPORTED);
        }
    }

    //posts/ + 랜덤 uuid + 확장자
    private String generateKey(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return "posts/" + UUID.randomUUID() + extension;
    }

    //이미지 url 생성
    private String buildImageUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
