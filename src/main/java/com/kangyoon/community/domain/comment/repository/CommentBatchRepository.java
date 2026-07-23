package com.kangyoon.community.domain.comment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CommentBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    // 댓글 좋아요수 변동사항(증감) 배치
    @Transactional
    public void batchIncreaseCommentLikeCount(Map<Long, Integer> deltaMap) {
        List<Object[]> params = deltaMap.entrySet().stream()
                .map(entry -> new Object[]{entry.getValue(), entry.getKey()})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE comment SET like_count = like_count + ? where id = ?",
                params
        );
    }

    // 지난 n일간 변동된 댓글의 정합성 배치(redis 증가 실패를 반영해주기 위함)
    @Transactional
    public void reconcileCommentLikeCounts(List<Long> commentIds) {
        if (commentIds.isEmpty()) return;

        List<Object[]> params = commentIds.stream()
                .map(id -> new Object[]{id})
                .toList();

        jdbcTemplate.batchUpdate(
                "UPDATE comment c SET c.like_count = " +
                        "(SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id = c.id) " +
                        "WHERE c.id = ?",
                params
        );
    }

}
