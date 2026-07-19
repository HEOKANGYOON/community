package com.kangyoon.community.infrastructure.redis;

import com.kangyoon.community.domain.board.repository.CommentBatchRepository;
import com.kangyoon.community.domain.post.repository.PostBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final PostBatchRepository postBatchRepository;
    private final CommentBatchRepository commentBatchRepository;


    private static final String VIEW_DELTA_KEY = "view:delta";
    private static final String RECOMMEND_DELTA_KEY = "recommend:delta";
    private static final String DISRECOMMEND_DELTA_KEY = "disrecommend:delta";
    private static final String COMMENT_LIKE_DELTA_KEY = "commentLike:delta";


    /* 증감 */

    // 조회수 증가
    public void increaseViewDelta(Long postId) {
        try {
            redisTemplate.opsForHash().increment(VIEW_DELTA_KEY, postId.toString(), 1);
        } catch (DataAccessException e) {
            log.warn("Redis 조회수 delta 반영 실패, postId={}", postId, e);
        }
    }

    // 추천 증가
    public void increaseRecommendDelta(Long postId) {
        try {
            redisTemplate.opsForHash().increment(RECOMMEND_DELTA_KEY, postId.toString(), 1);
        } catch (DataAccessException e) {
            log.warn("Redis 추천 delta 반영 실패, postId={}", postId, e);
        }
    }

    // 비추천 증가
    public void increaseDisrecommendDelta(Long postId) {
        try {
            redisTemplate.opsForHash().increment(DISRECOMMEND_DELTA_KEY, postId.toString(), 1);
        } catch (DataAccessException e) {
            log.warn("Redis 비추천 delta 반영 실패, postId={}", postId, e);
        }
    }

    // 댓글 좋아요 증가
    public void increaseCommentLikeDelta(Long commentId) {
        try {
            redisTemplate.opsForHash().increment(COMMENT_LIKE_DELTA_KEY, commentId.toString(), 1);
        } catch (DataAccessException e) {
            log.warn("Redis 댓글좋아요 delta 증가 실패, commentId={}", commentId, e);
        }
    }

    // 댓글 좋아요 취소
    public void decreaseCommentLikeDelta(Long commentId) {
        try {
            redisTemplate.opsForHash().increment(COMMENT_LIKE_DELTA_KEY, commentId.toString(), -1);
        } catch (DataAccessException e) {
            log.warn("Redis 댓글좋아요 delta 감소 실패, commentId={}", commentId, e);
        }
    }

    /* 단건 조회 */

    // 게시글 조회수 조회
    public int getViewDelta(Long postId) {
        try {
            Object value = redisTemplate.opsForHash().get(VIEW_DELTA_KEY, postId.toString());
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (DataAccessException e) {
            log.warn("Redis 조회수 delta 조회 실패, postId={}", postId, e);
            return 0;
        }
    }

    // 게시글 추천수 조회
    public int getRecommendDelta(Long postId) {
        try {
            Object value = redisTemplate.opsForHash().get(RECOMMEND_DELTA_KEY, postId.toString());
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (DataAccessException e) {
            log.warn("Redis 추천 delta 조회 실패, postId={}", postId, e);
            return 0;
        }
    }


    // 게시글 비추천수 조회
    public int getDisrecommendDelta(Long postId) {
        try {
            Object value = redisTemplate.opsForHash().get(DISRECOMMEND_DELTA_KEY, postId.toString());
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (DataAccessException e) {
            log.warn("Redis 비추천 delta 조회 실패, postId={}", postId, e);
            return 0;
        }
    }

    // 댓글 좋아요수 조회
    public int getCommentLikeDelta(Long commentId) {
        try {
            Object value = redisTemplate.opsForHash().get(COMMENT_LIKE_DELTA_KEY, commentId.toString());
            return value != null ? Integer.parseInt(value.toString()) : 0;
        } catch (DataAccessException e) {
            log.warn("Redis 댓글좋아요 delta 조회 실패, commentId={}", commentId, e);
            return 0;
        }
    }

    /* 리스트 조회 (목록용) */

    // 게시글 목록용 조회수 리스트
    public Map<Long, Integer> getViewDeltaList(List<Long> postIds) {
        return getDeltaListInternal(VIEW_DELTA_KEY, postIds);
    }

    // 게시글 목록용 추천수 리스트
    public Map<Long, Integer> getRecommendDeltaList(List<Long> postIds) {
        return getDeltaListInternal(RECOMMEND_DELTA_KEY, postIds);
    }

    // 댓글 목록용(게시글 내) 리스트
    public Map<Long, Integer> getCommentLikeDeltaList(List<Long> commentIds) {
        return getDeltaListInternal(COMMENT_LIKE_DELTA_KEY, commentIds);
    }


    /* 배치용 메서드 */

    // 배치용 회수 + DB 반영
    public void flushViewDeltaToDb() {
        flushDeltaToDb(VIEW_DELTA_KEY, postBatchRepository::batchIncreaseViewCount);
    }

    public void flushRecommendDeltaToDb() {
        flushDeltaToDb(RECOMMEND_DELTA_KEY, postBatchRepository::batchIncreaseRecommendCount);
    }

    public void flushDisrecommendDeltaToDb() {
        flushDeltaToDb(DISRECOMMEND_DELTA_KEY, postBatchRepository::batchIncreaseDisrecommendCount);
    }

    public void flushCommentLikeDeltaToDb() {
        flushDeltaToDb(COMMENT_LIKE_DELTA_KEY, commentBatchRepository::batchIncreaseCommentLikeCount);
    }


    /* 메서드 헬퍼 */

    // 배치 메서드 헬퍼
    private void flushDeltaToDb(String deltaKey, Consumer<Map<Long, Integer>> batchUpdate) {
        String processingKey = deltaKey + ":processing";

        try {
            redisTemplate.rename(deltaKey, processingKey);
        } catch (DataAccessException e) {
            log.info("[FLUSH] {} - rename 실패(변동없음으로 처리): {}", deltaKey, e.getMessage());
            return; // 변동 없음
        }

        Map<Object, Object> rawDeltas = redisTemplate.opsForHash().entries(processingKey);
        log.info("[FLUSH] {} - rawDeltas={}", deltaKey, rawDeltas);   // 여기 로그가 핵심
        if (rawDeltas.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        Map<Long, Integer> deltaMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawDeltas.entrySet()) {
            deltaMap.put(Long.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getValue().toString()));
        }
        log.info("[FLUSH] {} - deltaMap={}", deltaKey, deltaMap);   // 파싱 결과 확인

        try {
            batchUpdate.accept(deltaMap);
            log.info("[FLUSH] {} - DB 반영 완료", deltaKey);   // 성공 여부 확인
            redisTemplate.delete(processingKey);
        } catch (Exception e) {
            try {
                log.error("[FLUSH] {} - DB 반영 실패, 원인: ", deltaKey, e);   // 실패 시 진짜 원인
                deltaMap.forEach((id, delta) ->
                        redisTemplate.opsForHash().increment(deltaKey, id.toString(), delta));
                redisTemplate.delete(processingKey);
            } catch (DataAccessException rollbackFailure) {
                e.addSuppressed(rollbackFailure);
                log.error("delta 롤백 실패, 데이터 유실 가능성. key={}, deltaMap={}", deltaKey, deltaMap, rollbackFailure);
            }
            throw e;
        }
    }

    // 목록 조회용 메서드 헬퍼
    private Map<Long, Integer> getDeltaListInternal(String deltaKey, List<Long> ids) {
        Map<Long, Integer> result = new HashMap<>();
        List<Object> hashKeys = ids.stream().map(id -> (Object) id.toString()).toList();

        List<Object> values;
        try {
            values = redisTemplate.opsForHash().multiGet(deltaKey, hashKeys);
        } catch (DataAccessException e) {
            log.warn("Redis delta 다건 조회 실패, key={}", deltaKey, e);
            values = Collections.nCopies(ids.size(), null);
        }

        for (int i = 0; i < ids.size(); i++) {
            Object value = values.get(i);
            result.put(ids.get(i), value != null ? Integer.parseInt(value.toString()) : 0);
        }
        return result;
    }



}

