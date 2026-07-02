package com.kangyoon.community.domain.board.controller;

import com.kangyoon.community.domain.board.dto.BoardCreateRequest;
import com.kangyoon.community.domain.board.dto.BoardResponse;
import com.kangyoon.community.domain.board.dto.BoardUpdateRequest;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.service.BoardService;
import com.kangyoon.community.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Tag(name = "Board", description = "Board API")
    @Operation(
            summary = "게시판 목록 조회",
            description = "게시판 목록을 조회합니다."
    )
    @GetMapping("/api/boards")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards() {
        List<BoardResponse> boards = boardService.getBoards();
        return ResponseEntity.ok(new ApiResponse<>("조회 성공", boards));
    }

    @Tag(name = "Board", description = "Board API")
    @Operation(
            summary = "게시판 정보 조회",
            description = "게시판 id에 해당하는 게시판 정보를 조회합니다."
    )
    @GetMapping("/api/boards/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> findBoardById(@PathVariable Long id) {
        BoardResponse board = boardService.findBoard(id);
        return ResponseEntity.ok(new ApiResponse<>("조회 성공", board));
    }

    @Tag(name = "ADMIN_Board", description = "ADMIN_Board API")
    @Operation(
            summary = "게시판 생성",
            description = "게시판을 생성합니다."
    )
    @PostMapping("/api/admin/boards")
    public ResponseEntity<ApiResponse<Void>> createBoard(@RequestBody @Valid BoardCreateRequest request) {
        boardService.createBoard(request.name(), request.description(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시판 생성 성공", null));
    }

    @Tag(name = "ADMIN_Board", description = "ADMIN_Board API")
    @Operation(
            summary = "게시판 수정",
            description = "게시판 정보를 수정합니다."
    )
    @PatchMapping("/api/admin/boards/{boardId}")
    public ResponseEntity<ApiResponse<Void>> updateBoard(@PathVariable Long boardId, @RequestBody @Valid BoardUpdateRequest request) {
        boardService.updateBoard(boardId, request.name(), request.description(), request.categoryId());
        return ResponseEntity.ok()
                .body(new ApiResponse<>("게시판 수정 성공", null));
    }

    @Tag(name = "ADMIN_Board", description = "ADMIN_Board API")
    @Operation(
            summary = "게시판 삭제",
            description = "게시판을 삭제합니다."
    )
    @DeleteMapping("/api/admin/boards/{boardId}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.ok()
                .body(new ApiResponse<>("게시판 삭제 성공", null));
    }

}
