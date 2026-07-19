package com.kangyoon.community.domain.post.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PostBatchRepository{

    private final JdbcTemplate jdbcTemplate;

    // 게시글 조회수 변동(증가분) 배치
    public void batchIncreaseViewCount(Map<Long, Integer> deltaMap) {
        List<Object[]> params = deltaMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET view_count = view_count + ? where id = ?",
                params
        );
    }

    // 게시글 추천수 변동(증가분) 배치
    public void batchIncreaseRecommendCount(Map<Long, Integer> deltaMap) {
        List<Object[]> params = deltaMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET recommendation_count = recommendation_count + ? where id = ?",
                params
        );
    }

    // 게시글 비추천수 변동(증가분) 배치
    public void batchIncreaseDisrecommendCount(Map<Long, Integer> deltaMap) {
        List<Object[]> params = deltaMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET disrecommendation_count = disrecommendation_count + ? where id = ?",
                params
        );
    }

}
