package com.kangyoon.community.domain.board.repository;

import com.kangyoon.community.domain.board.entity.BoardManager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardManagerRepository extends JpaRepository<BoardManager, Long> {
    Boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
}
