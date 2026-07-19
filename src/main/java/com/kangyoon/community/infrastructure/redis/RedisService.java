package com.kangyoon.community.infrastructure.redis;

import com.kangyoon.community.domain.comment.repository.CommentBatchRepository;
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
        if (rawDeltas.isEmpty()) {
            redisTemplate.delete(processingKey);
            return;
        }

        Map<Long, Integer> deltaMap = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawDeltas.entrySet()) {
            deltaMap.put(Long.valueOf(entry.getKey().toString()), Integer.valueOf(entry.getValue().toString()));
        }
        log.info("[FLUSH] {} - deltaMap={}", deltaKey, deltaMap);

        // 1단계: DB 반영 실패하면 deltaMap을 원래 키(deltaKey)로 복구해야 함 (진짜 롤백)
        try {
            batchUpdate.accept(deltaMap);
            log.info("[FLUSH] {} - DB 반영 완료", deltaKey);
        } catch (Exception dbException) {
            log.error("[FLUSH] {} - DB 반영 실패, 원인: ", deltaKey, dbException);

            // 1-1단계: 복구 (실패하면 진짜 데이터 유실)
            try {
                deltaMap.forEach((id, delta) ->
                        redisTemplate.opsForHash().increment(deltaKey, id.toString(), delta));
            } catch (DataAccessException restoreFailure) {
                dbException.addSuppressed(restoreFailure);
                log.error("delta 복구 실패, 데이터 유실 가능성 있음. key={}, deltaMap={}", deltaKey, deltaMap, restoreFailure);
                throw dbException;
            }

            // 1-2단계: processingKey 청소 (복구는 이미 성공했으므로 실패해도 괜찮음, 다음 RENAME이 덮어씀)
            try {
                redisTemplate.delete(processingKey);
            } catch (DataAccessException cleanupFailure) {
                log.warn("[FLUSH] {} - 롤백 복구는 성공했으나 processingKey 청소 실패. 다음 배치의 RENAME이 자동 정리함. processingKey={}",
                        deltaKey, processingKey, cleanupFailure);
            }

            throw dbException;
        }

        // 2단계: DB 반영은 이미 성공 processingKey 삭제만 남음. 실패해도 deltaMap을 되돌리면 안 됨(이중 가산 방지)
        try {
            redisTemplate.delete(processingKey);
        } catch (DataAccessException deleteException) {
            log.warn("[FLUSH] {} - DB 반영은 성공했으나 processingKey 삭제 실패. 다음 배치의 RENAME이 자동 정리함. processingKey={}",
                    deltaKey, processingKey, deleteException);
            // 롤백 없음, throw 없음 — DB는 이미 확정, 정상 흐름으로 종료
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

