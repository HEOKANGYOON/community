package com.kangyoon.community.domain.post.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PostBatchRepository{

    private final JdbcTemplate jdbcTemplate;

    // 게시글 조회수 변동(증가분) 배치
    @Transactional
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
    @Transactional
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
    @Transactional
    public void batchIncreaseDisrecommendCount(Map<Long, Integer> deltaMap) {
        List<Object[]> params = deltaMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post SET disrecommendation_count = disrecommendation_count + ? where id = ?",
                params
        );
    }


    // 지난 n일간 변동된 게시글 추천수의 정합성 배치(redis 증가 실패를 반영해주기 위함)
    @Transactional
    public void reconcileRecommendCounts(List<Long> postIds) {
        if (postIds.isEmpty()) return;

        List<Object[]> params = postIds.stream()
                .map(id -> new Object[]{id})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post p SET p.recommendation_count = " +
                        "(SELECT COUNT(*) FROM post_vote pv WHERE pv.post_id = p.id AND pv.vote_type = 'UP') " +
                        "WHERE p.id = ?",
                params
        );
    }
    // 지난 n일간 변동된 게시글 비추천수의 정합성 배치(redis 증가 실패를 반영해주기 위함)
    @Transactional
    public void reconcileDisrecommendCounts(List<Long> postIds) {
        if (postIds.isEmpty()) return;

        List<Object[]> params = postIds.stream()
                .map(id -> new Object[]{id})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE post p SET p.disrecommendation_count = " +
                        "(SELECT COUNT(*) FROM post_vote pv WHERE pv.post_id = p.id AND pv.vote_type = 'DOWN') " +
                        "WHERE p.id = ?",
                params
        );
    }

}
