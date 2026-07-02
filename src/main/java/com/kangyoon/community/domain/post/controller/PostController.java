package com.kangyoon.community.domain.post.controller;

import com.kangyoon.community.domain.post.dto.*;
import com.kangyoon.community.domain.post.service.PostService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.common.PageResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import com.kangyoon.community.infrastructure.s3.S3Service;
import com.kangyoon.community.infrastructure.s3.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final S3Service s3Service;

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 목록 조회 및 게시글 검색",
            description = "선택한 게시판의 게시글 목록을 조회하고, 주어진 키워드 및 검색 타입으로 조건에 맞는 게시글을 조회합니다."
    )
    @GetMapping("/api/boards/{boardId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getPostList(
            @PathVariable Long boardId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String searchType,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<PostSummaryResponse> postList = postService.getAllPost(boardId, keyword, searchType, pageable);
        return ResponseEntity.ok(new ApiResponse<>("리스트 조회 성공", PageResponse.from(postList)));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 조회",
            description = "선택한 게시판의 게시글을 조회합니다."
    )
    @GetMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long boardId, @PathVariable Long postId) {
        PostResponse postResponse = postService.getPost(postId);

        return ResponseEntity.ok(new ApiResponse<PostResponse>("게시글 조회 성공", postResponse));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 작성",
            description = "선택한 게시판에 게시글을 작성합니다."
    )
    @PostMapping("/api/boards/{boardId}/posts")
    public ResponseEntity<ApiResponse<PostResponse>> writePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @RequestBody @Valid PostCreateRequest request) {

        PostResponse postResponse = postService.writePost(userDetails.getMemberId(), boardId, request.title(), request.content());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시글 작성 성공", postResponse));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 수정",
            description = "선택한 게시판의 게시글을 수정합니다."
    )
    @PutMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> editPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request) {

        PostResponse postResponse = postService.editPost(postId, userDetails.getMemberId(), request.title(), request.content());

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postResponse));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 삭제",
            description = "선택한 게시판의 게시글을 삭제합니다."
    )
    @DeleteMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId) {

        postService.deletePost(postId, userDetails.getMemberId(), userDetails.getRole());
        return ResponseEntity.ok()
                .body(new ApiResponse<>("게시글 삭제 성공", null));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "게시글 추천",
            description = "선택한 게시판의 게시글을 추천또는 비추천합니다.(하나의 선택만 가능, 취소 불가)"
    )
    @PostMapping("/api/boards/{boardId}/posts/{postId}/vote")
    public ResponseEntity<ApiResponse<Void>> votePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody VoteRequest request
    ) {
        postService.vote(userDetails.getMemberId(), postId, request.voteType());
        return ResponseEntity.ok()
                .body(new ApiResponse<>("게시글 추천/비추천 성공", null));
    }

    @Tag(name = "Post", description = "Post API")
    @Operation(
            summary = "S3 이미지 업로드 URL 발급용 API",
            description = "S3에 이미지를 저장하기 위한 업로드용 URL을 발급합니다."
    )
    @PostMapping("/api/boards/{boardId}/posts/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PresignedUrlRequest request
            ) {
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request.fileName(), request.contentType());
        return ResponseEntity.ok(new ApiResponse<>("Presigned URL 발급 성공", response));
    }
}
