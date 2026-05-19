package com.kangyoon.community.domain.board.repository;

import com.kangyoon.community.domain.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardRepositoryCustom {
    List<Board> findByDeletedAtIsNull();
    Optional<Board> findByIdAndDeletedAtIsNull(Long id);
    Boolean existsByName(String name);
}
