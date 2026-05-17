package com.kangyoon.community.domain.board.repository;

import com.kangyoon.community.domain.board.entity.BoardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCategoryRepository extends JpaRepository<BoardCategory, Long> {
}
