package com.kangyoon.community.domain.comment.controller;

import com.kangyoon.community.domain.comment.dto.CommentDetailResponse;
import com.kangyoon.community.domain.comment.dto.CommentWriteRequest;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.service.CommentService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.common.PageResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/api/boards/{boardId}/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentsResponse>>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable
            ) {
        Page<CommentsResponse> comments = commentService.getComments(postId, pageable);
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", PageResponse.from(comments)));
    }

    @GetMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDetailResponse>> getCommentDetail(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        CommentDetailResponse commentDetail = commentService.commentDetail(commentId);
        return ResponseEntity.ok(new ApiResponse<>("댓글 상세 조회 성공", commentDetail));
    }

    @PostMapping("/api/boards/{boardId}/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> commentWrite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @RequestBody @Valid CommentWriteRequest request
            ) {

        commentService.commentWrite(postId, userDetails.getMemberId(), request.content());

        return ResponseEntity.ok(new ApiResponse<>("댓글 작성 성공", null));
    }

    @PostMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<Void>> replyWrite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentWriteRequest request
    ) {

        commentService.replyWrite(postId, userDetails.getMemberId(), commentId, request.content());

        return ResponseEntity.ok(new ApiResponse<>("대댓글 작성 성공", null));
    }

    @PatchMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> commentEdit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentWriteRequest request
    ) {
        commentService.commentEdit(commentId, userDetails.getMemberId(), request.content());
        return ResponseEntity.ok(new ApiResponse<>("댓글 수정 성공", null));
    }

    @DeleteMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> commentDelete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId
            ) {
        commentService.commentDelete(commentId, userDetails.getMemberId(), boardId, userDetails.getRole());
        return ResponseEntity.ok(new ApiResponse<>("댓글 삭제 성공", null));
    }

    @PostMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Void>> commentLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.commentLikeToggle(commentId, userDetails.getMemberId(), postId);
        return ResponseEntity.ok(new ApiResponse<>("댓글 좋아요 토글 성공", null));
    }

}
