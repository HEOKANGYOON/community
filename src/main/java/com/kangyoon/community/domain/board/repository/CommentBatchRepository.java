package com.kangyoon.community.domain.board.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CommentBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void updateCommentLike(Map<Long, Integer> countMap) {
        List<Object[]> params = countMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE comment SET like_count = ? where id = ?",
                params
        );
    }
}
