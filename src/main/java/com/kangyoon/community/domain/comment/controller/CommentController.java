package com.kangyoon.community.domain.comment.controller;

import com.kangyoon.community.domain.comment.dto.CommentDetailResponse;
import com.kangyoon.community.domain.comment.dto.CommentWriteRequest;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.service.CommentService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.common.PageResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글 조회",
            description = "선택한 게시판의 게시글의 댓글들을 조회합니다.(페이징)"
    )
    @GetMapping("/api/boards/{boardId}/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentsResponse>>> getComments(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable
            ) {
        Page<CommentsResponse> comments = commentService.getComments(postId, pageable);
        return ResponseEntity.ok(new ApiResponse<>("댓글 조회 성공", PageResponse.from(comments)));
    }

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글 상세 조회",
            description = "선택한 게시판의 게시글의 댓글 중 선택한 댓글의 정보를 상세 조회합니다."
    )
    @GetMapping("/api/boards/{boardId}/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentDetailResponse>> getCommentDetail(
            @PathVariable Long boardId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        CommentDetailResponse commentDetail = commentService.commentDetail(commentId);
        return ResponseEntity.ok(new ApiResponse<>("댓글 상세 조회 성공", commentDetail));
    }

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글 작성",
            description = "선택한 게시판의 게시글에 댓글을 작성합니다."
    )
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

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "대댓글 작성",
            description = "선택한 게시판의 게시글에 댓글에 대댓글을 작성합니다."
    )
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

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글(대댓글) 수정",
            description = "선택한 게시판의 게시글에 댓글(대댓글)을 수정합니다.(자신의 댓글/대댓글만 수정 가능)"
    )
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

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글(대댓글) 삭제",
            description = "선택한 게시판의 게시글의 댓글(대댓글)을 삭제합니다.(자신의 댓글/대댓글만 삭제 가능)"
    )
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

    @Tag(name = "Comment", description = "Comment API")
    @Operation(
            summary = "댓글 좋아요",
            description = "선택한 게시판의 게시글의 댓글에 좋아요를 표시합니다.(토클, 취소 가능)"
    )
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
