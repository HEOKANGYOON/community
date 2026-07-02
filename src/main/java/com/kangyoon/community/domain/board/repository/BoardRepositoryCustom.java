package com.kangyoon.community.domain.board.repository;

import com.kangyoon.community.domain.board.entity.Board;

import java.util.List;

public interface BoardRepositoryCustom {
    List<Board> findAllActiveBoards();
}
