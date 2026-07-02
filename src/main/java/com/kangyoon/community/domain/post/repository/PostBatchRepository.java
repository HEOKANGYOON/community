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

    public void batchUpdateViewCount(Map<Long, Integer> countMap) {
        List<Object[]> parms = countMap.entrySet().stream()
                .map(entry ->new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET view_count = ? where id = ?",
                parms
        );
    }

    public void batchUpdateRecommendCount(Map<Long, Integer> countMap) {
        List<Object[]> parms = countMap.entrySet().stream()
                .map(entry ->new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET recommendation_count = ? where id = ?",
                parms
        );
    }


    public void batchUpdateDisrecommendCount(Map<Long, Integer> countMap) {
        List<Object[]> parms = countMap.entrySet().stream()
                .map(entry ->new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET disrecommendation_count = ? where id = ?",
                parms
        );
    }

}
