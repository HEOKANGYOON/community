package com.kangyoon.community.infrastructure.redis;

import com.kangyoon.community.domain.board.repository.CommentBatchRepository;
import com.kangyoon.community.domain.comment.repository.CommentLikeRepository;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.VoteType;
import com.kangyoon.community.domain.post.repository.PostBatchRepository;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.domain.post.repository.PostVoteRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;
    private final PostVoteRepository postVoteRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostBatchRepository postBatchRepository;
    private final CommentBatchRepository commentBatchRepository;

    private static final String POST_RECOMMEND_KEY = "post:recommend:";
    private static final String POST_DISRECOMMEND_KEY = "post:disrecommend:";
    private static final String POST_VIEWCOUNT_KEY = "post:viewcount:";
    private static final String COMMENT_LIKE_KEY = "comment:like:";


    public int getRecommendCount(Long postId) {
        String value = redisTemplate.opsForValue().get(POST_RECOMMEND_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        int recommendCount = postVoteRepository.countByPostIdAndVoteType(postId, VoteType.UP);

        redisTemplate.opsForValue().set(POST_RECOMMEND_KEY + postId, String.valueOf(recommendCount));
        return recommendCount;
    }

    public int getDisrecommendCount(Long postId) {
        String value = redisTemplate.opsForValue().get(POST_DISRECOMMEND_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        int disrecommendCount = postVoteRepository.countByPostIdAndVoteType(postId, VoteType.DOWN);

        redisTemplate.opsForValue().set(POST_DISRECOMMEND_KEY + postId, String.valueOf(disrecommendCount));
        return disrecommendCount;
    }

    public int getViewCount(Long postId) {
        //조회수의 경우는 가장 최신 스냅샷(가장 최근에 배치한것)으로 데이터 복구
        String value = redisTemplate.opsForValue().get(POST_VIEWCOUNT_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        redisTemplate.opsForValue().set(POST_VIEWCOUNT_KEY + postId, String.valueOf(post.getViewCount()));
        return post.getViewCount();
    }



    public void increaseRecommend(Long postId) {
        redisTemplate.opsForValue().increment(POST_RECOMMEND_KEY + postId);
    }

    public void increaseDisrecommend(Long postId) {
        redisTemplate.opsForValue().increment(POST_DISRECOMMEND_KEY + postId);
    }

    public void increaseViewCount(Long postId) {
        redisTemplate.opsForValue().increment(POST_VIEWCOUNT_KEY + postId);
    }

    public void increaseCommentLike(Long commentId) {
        redisTemplate.opsForValue().increment(COMMENT_LIKE_KEY + commentId);
    }

    public void decreaseCommentLike(Long commentId) {
        redisTemplate.opsForValue().decrement(COMMENT_LIKE_KEY + commentId);
    }


    public Map<Long, Integer> getViewCountList(List<Long> postIds) {
        List<String> keys = postIds.stream()
                .map(id -> POST_VIEWCOUNT_KEY + id)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        //응답으로 보낼 해시 맵
        Map<Long, Integer> result = new HashMap<>();

        //redis에 적재되지 않은 post들
        List<Long> missedIds = new ArrayList<>();


        for (int i = 0; i < postIds.size(); i++){
            String value = values.get(i);

            if (value != null) {
                result.put(postIds.get(i), Integer.parseInt(value));
            } else {
                missedIds.add(postIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            postRepository.findAllById(missedIds)
                    .forEach(post -> {
                        //redis에 적재한 후
                        redisTemplate.opsForValue().set(
                                POST_VIEWCOUNT_KEY + post.getId(),
                                String.valueOf(post.getViewCount()));

                        //가장 최신의 배치된 데이터를 응답으로
                        result.put(post.getId(), post.getViewCount());
                    });
        }

        return result;
    }

    public Map<Long, Integer> getRecommendList(List<Long> postIds) {

        List<String> keys = postIds.stream()
                .map(id -> POST_RECOMMEND_KEY + id)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Integer> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        for (int i = 0; i < postIds.size(); i++) {
            String value = values.get(i);

            if (value != null) {
                result.put(postIds.get(i), Integer.parseInt(value));
            } else {
                missedIds.add(postIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {
            Map<Long, Integer> dbCounts = postVoteRepository.countByPostIdsAndVoteType(missedIds, VoteType.UP);

            missedIds.forEach(postId -> {
                int count = dbCounts.getOrDefault(postId, 0);
                redisTemplate.opsForValue().set(
                        POST_RECOMMEND_KEY + postId,
                        String.valueOf(count)
                );
                result.put(postId, count);
            });
        }
        return result;
    }

    public Map<Long, Integer> getCommentLikeList(List<Long> commentIds) {

        List<String> keys = commentIds.stream()
                .map(id -> COMMENT_LIKE_KEY + id)
                .toList();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        Map<Long, Integer> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();


        for (int i = 0; i < commentIds.size(); i++) {
            String value = values.get(i);

            if (value != null) {
                result.put(commentIds.get(i), Integer.parseInt(value));
            } else {
                missedIds.add(commentIds.get(i));
            }
        }

        if (!missedIds.isEmpty()) {

            Map<Long, Integer> dbCounts = commentLikeRepository.countByCommentIds(missedIds);

            missedIds.forEach(commentId -> {
                int count = dbCounts.getOrDefault(commentId, 0);

                redisTemplate.opsForValue().set(
                        COMMENT_LIKE_KEY + commentId,
                        String.valueOf(count)
                        );
                result.put(commentId, count);
            });
        }
        return result;
    }

    // TODO: 변동 없는 키도 매 배치마다 UPDATE 발생
    // 개선안: 배치 후 Redis 키 삭제 또는 변동 감지 로직 추가 (개선안임 하겠다는 소리 아님)
    public void syncViewCountsToDB() {
        Map<Long, Integer> countMap = scanToMap(POST_VIEWCOUNT_KEY);
        if (!countMap.isEmpty()) {
            postBatchRepository.batchUpdateViewCount(countMap);
        }
    }

    public void syncRecommendCountsToDB() {
        Map<Long, Integer> countMap = scanToMap(POST_RECOMMEND_KEY);
        if (!countMap.isEmpty()) {
            postBatchRepository.batchUpdateRecommendCount(countMap);
        }
    }


    public void syncDisrecommendCountsToDB() {
        Map<Long, Integer> countMap = scanToMap(POST_DISRECOMMEND_KEY);
        if (!countMap.isEmpty()) {
            postBatchRepository.batchUpdateDisrecommendCount(countMap);
        }
    }

    public void syncCommentLikeCountsToDB() {
        Map<Long, Integer> countMap = scanToMap(COMMENT_LIKE_KEY);
        if (!countMap.isEmpty()) {
            commentBatchRepository.updateCommentLike(countMap);
        }
    }


    private Map<Long, Integer> scanToMap(String keyPattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(keyPattern + "*")
                .count(100)
                .build();

        Map<Long, Integer> countMap = new HashMap<>();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String value = redisTemplate.opsForValue().get(key);

                if (value == null) {
                    continue;
                }

                Long Id = Long.parseLong(key.replace(keyPattern, ""));
                countMap.put(Id, Integer.parseInt(value));
            }
        }
        return countMap;
    }


}

