package com.kangyoon.community.domain.board.controller;

import com.kangyoon.community.domain.board.dto.BoardCreateRequest;
import com.kangyoon.community.domain.board.dto.BoardResponse;
import com.kangyoon.community.domain.board.dto.BoardUpdateRequest;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.service.BoardService;
import com.kangyoon.community.global.common.ApiResponse;
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

    @GetMapping("/api/boards")
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards() {
        List<BoardResponse> boards = boardService.getBoards();
        return ResponseEntity.ok(new ApiResponse<>("조회 성공", boards));
    }

    @GetMapping("/api/boards/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> findBoardById(@PathVariable Long id) {
        BoardResponse board = boardService.findBoard(id);
        return ResponseEntity.ok(new ApiResponse<>("조회 성공", board));
    }

    @PostMapping("/api/admin/boards")
    public ResponseEntity<ApiResponse<Void>> createBoard(@RequestBody @Valid BoardCreateRequest request) {
        boardService.createBoard(request.name(), request.description(), request.categoryId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("게시판 생성 성공", null));
    }

    @PatchMapping("/api/admin/boards/{boardId}")
    public ResponseEntity<Void> updateBoard(@PathVariable Long boardId, @RequestBody @Valid BoardUpdateRequest request) {
        boardService.updateBoard(boardId, request.name(), request.description(), request.categoryId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/boards/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId) {
        boardService.deleteBoard(boardId);
        return ResponseEntity.noContent().build();
    }

}
