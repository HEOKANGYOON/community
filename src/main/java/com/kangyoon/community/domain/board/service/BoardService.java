package com.kangyoon.community.domain.board.service;

import com.kangyoon.community.domain.board.dto.BoardResponse;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.board.repository.BoardCategoryRepository;
import com.kangyoon.community.domain.board.repository.BoardManagerRepository;
import com.kangyoon.community.domain.board.repository.BoardRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardManagerRepository boardManagerRepository;

    public List<BoardResponse> getBoards() {
        return boardRepository.findByDeletedAtIsNull()
                .stream()
                .map(BoardResponse::from)
                .toList();
    }

    public BoardResponse findBoard(Long boardId) {
        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                        .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        return BoardResponse.from(board);
    }

    //생성 권한은 컨트롤러 어노테이션으로 해결
    public void createBoard(String name, String description, Long categoryId) {

        BoardCategory category = boardCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));

        if (boardRepository.existsByName(name)) {
            throw new CustomException(ErrorCode.DUPLICATE_BOARD_NAME);
        }

        Board board = Board.create(name, description, category);

        boardRepository.save(board);
    }

    //게시판의 C, U, D 권한은 ADMIN만 가능
    public void updateBoard(Long boardId, String name, String description, Long boardCategoryId) {

        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        if (name != null && !name.isBlank()) {
            board.updateName(name);
        }

        if (description != null && !description.isBlank()) {
            board.updateDescription(description);
        }

        if (boardCategoryId != null) {
            BoardCategory category = boardCategoryRepository.findById(boardCategoryId)
                            .orElseThrow(() -> new CustomException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));
            board.updateCategory(category);
        }
    }

    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        board.deleteBoard();
    }
}
