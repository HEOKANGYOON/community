package com.kangyoon.community.infrastructure.s3;

import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.infrastructure.s3.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
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
    private final S3Client s3Client;

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

    //temp/ + 랜덤 uuid + 확장자
    private String generateKey(String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        //클라이언트에서 이미지를 업로드 할 때 temp/로 저장되게 함(고아 이미지 관리 때매)
        return "temp/" + UUID.randomUUID() + extension;
    }

    //이미지 url 생성
    private String buildImageUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    // URL -> key 역변환
    private String extractKeyFromUrl(String imageUrl) {
        String prefix = buildImageUrl(""); // "https://bucket.s3.region.amazonaws.com/"
        if (!imageUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid S3 URL: " + imageUrl);
        }
        return imageUrl.substring(prefix.length()); // "temp/abc-123.jpg"
    }

    // temp -> post 이동, URL 받아서 URL 반환
    public String moveToPostFolder(String tempUrl) {
        String tempKey = extractKeyFromUrl(tempUrl);
        String postKey = tempKey.replaceFirst("^temp/", "posts/");

        //temp -> posts 복사
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(tempKey)
                .destinationBucket(bucket)
                .destinationKey(postKey)
                .build();
        s3Client.copyObject(copyRequest);

        //temp에 있던 것 삭제
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(tempKey)
                .build();
        s3Client.deleteObject(deleteRequest);

        return buildImageUrl(postKey);
    }

    // 삭제도 URL 기준으로 받도록 (removed 처리용)
    // 게시글 수정 시 삭제한 이미지 버킷에서 제거
    public void deleteObject(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

}
