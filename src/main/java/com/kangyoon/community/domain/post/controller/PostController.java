package com.kangyoon.community.domain.post.controller;

import com.kangyoon.community.domain.post.dto.PostCreateRequest;
import com.kangyoon.community.domain.post.dto.PostResponse;
import com.kangyoon.community.domain.post.dto.PostSummaryResponse;
import com.kangyoon.community.domain.post.dto.PostUpdateRequest;
import com.kangyoon.community.domain.post.service.PostService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/api/boards/{boardId}/posts")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> getPostList(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue =  "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<PostSummaryResponse> postList = postService.getAllPost(boardId, pageable);
        return ResponseEntity.ok(new ApiResponse<>("리스트 조회 성공", postList));
    }

    @GetMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(@PathVariable Long boardId, @PathVariable Long postId) {
        PostResponse postResponse = postService.getPost(postId);

        return ResponseEntity.ok(new ApiResponse<PostResponse>("게시글 조회 성공", postResponse));
    }

    @PostMapping("/api/boards/{boardId}/posts")
    public ResponseEntity<ApiResponse<PostResponse>> writePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @RequestBody @Valid PostCreateRequest request) {

        PostResponse postResponse = postService.writePost(userDetails.getMemberId(), boardId, request.title(), request.content());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시글 작성 성공", postResponse));
    }

    @PutMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> editPost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody @Valid PostUpdateRequest request) {

        PostResponse postResponse = postService.editPost(postId, userDetails.getMemberId(), request.title(), request.content());

        return ResponseEntity.ok(new ApiResponse<>("게시글 수정 성공", postResponse));
    }

    @DeleteMapping("/api/boards/{boardId}/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId) {

        postService.deletePost(postId, userDetails.getMemberId(), userDetails.getRole());
        return ResponseEntity.ok()
                .body(new ApiResponse<>("게시글 삭제 성공", null));
    }

}
