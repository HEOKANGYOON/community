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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
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

    private static final String DIRTY_VIEWCOUNT_KEY = "dirty:viewcount";
    private static final String DIRTY_RECOMMEND_KEY = "dirty:recommend";
    private static final String DIRTY_DISRECOMMEND_KEY = "dirty:disrecommend";
    private static final String DIRTY_COMMENT_LIKE_KEY = "dirty:commentlike";

    //캐시 미싱에 해당 하는 부분만 있고 redis 장애 시의 try-catch문 빠짐
    //redis 장애 시 동시 다발적으로 count 쿼리 날리면 트래픽 급증함
    //post테이블의 최신 스냅샷을 반환하도록 하고 redis에 적재는 안함 redis가 정상적으로 돌아 왔을때 캐시 미싱 된것 처리하도록
    public int getRecommendCount(Long postId) {
        String value = safeGet(POST_RECOMMEND_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        //캐시 미스의 경우에만 DB에서 count
        int recommendCount = postVoteRepository.countByPostIdAndVoteType(postId, VoteType.UP);

        //적재 시도, 장애 시 적재 안됨
        safeSet(POST_RECOMMEND_KEY + postId, String.valueOf(recommendCount));
        return recommendCount;
    }

    public int getDisrecommendCount(Long postId) {
        String value = safeGet(POST_DISRECOMMEND_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        int disrecommendCount = postVoteRepository.countByPostIdAndVoteType(postId, VoteType.DOWN);

        safeSet(POST_DISRECOMMEND_KEY + postId, String.valueOf(disrecommendCount));
        return disrecommendCount;
    }

    public int getViewCount(Long postId) {
        //조회수의 경우는 가장 최신 스냅샷(가장 최근에 배치한것)으로 데이터 복구
        String value = safeGet(POST_VIEWCOUNT_KEY + postId);

        if (value != null) {
            return Integer.parseInt(value);
        }

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        safeSet(POST_VIEWCOUNT_KEY + postId, String.valueOf(post.getViewCount()));
        return post.getViewCount();
    }



    //incr, decr 후 배치를 위한 dirty set에 저장
    public void increaseRecommend(Long postId) {
        try {
            redisTemplate.opsForValue().increment(POST_RECOMMEND_KEY + postId);
            redisTemplate.opsForSet().add(DIRTY_RECOMMEND_KEY, String.valueOf(postId));
        } catch (DataAccessException e) {
            log.warn("Redis 추천 반영 실패 (INCR 또는 SADD), postId={}", postId, e);
        }
    }

    public void increaseDisrecommend(Long postId) {
        try {
            redisTemplate.opsForValue().increment(POST_DISRECOMMEND_KEY + postId);
            redisTemplate.opsForSet().add(DIRTY_DISRECOMMEND_KEY, String.valueOf(postId));
        } catch (DataAccessException e) {
            log.warn("Redis 비추천 반영 실패, postId={}", postId, e);
        }
    }

    public void increaseViewCount(Long postId) {
        try {
            redisTemplate.opsForValue().increment(POST_VIEWCOUNT_KEY + postId);
            redisTemplate.opsForSet().add(DIRTY_VIEWCOUNT_KEY, String.valueOf(postId));
        } catch (DataAccessException e) {
            log.warn("Redis 조회수 반영 실패, postId={}", postId, e);
        }
    }

    public void increaseCommentLike(Long commentId) {
        try {
            redisTemplate.opsForValue().increment(COMMENT_LIKE_KEY + commentId);
            redisTemplate.opsForSet().add(DIRTY_COMMENT_LIKE_KEY, String.valueOf(commentId));
        } catch (DataAccessException e) {
            log.warn("Redis 댓글좋아요 반영 실패, commentId={}", commentId, e);
        }
    }

    public void decreaseCommentLike(Long commentId) {
        try {
            redisTemplate.opsForValue().decrement(COMMENT_LIKE_KEY + commentId);
            redisTemplate.opsForSet().add(DIRTY_COMMENT_LIKE_KEY, String.valueOf(commentId));
        } catch (DataAccessException e) {
            log.warn("Redis 댓글좋아요 취소 반영 실패, commentId={}", commentId, e);
        }
    }


    // Redis 장애 시 fallback이 없는 문제는 여기도 동일하게 적용
    public Map<Long, Integer> getViewCountList(List<Long> postIds) {
        List<String> keys = postIds.stream()
                .map(id -> POST_VIEWCOUNT_KEY + id)
                .toList();

        List<String> values = safeMultiGet(keys);
        if (values == null) {   //redis 커넥션 장애 등 NPE 방지
            values = Collections.nCopies(keys.size(), null);
        }

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
                        //가장 최신의 배치된 데이터를 응답으로 넣고
                        result.put(post.getId(), post.getViewCount());

                        //redis에 적재
                        //응답 후 적재로 순서 중요 redis가 장애나도 DB에서 가져온 응답은 되도록
                        safeSet(POST_VIEWCOUNT_KEY + post.getId(), String.valueOf(post.getViewCount()));
                    });
        }

        return result;
    }

    public Map<Long, Integer> getRecommendList(List<Long> postIds) {

        List<String> keys = postIds.stream()
                .map(id -> POST_RECOMMEND_KEY + id)
                .toList();

        List<String> values = safeMultiGet(keys);
        if (values == null) {   //redis 커넥션 장애 등 NPE 방지
            values = Collections.nCopies(keys.size(), null);
        }

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

                //응답 저장 후 redis 적재
                result.put(postId, count);
                safeSet(POST_RECOMMEND_KEY + postId, String.valueOf(count));
            });
        }
        return result;
    }

    public Map<Long, Integer> getCommentLikeList(List<Long> commentIds) {

        List<String> keys = commentIds.stream()
                .map(id -> COMMENT_LIKE_KEY + id)
                .toList();

        //MGET keys로
        List<String> values = safeMultiGet(keys);
        if (values == null) {   //redis 커넥션 장애 등 NPE 방지
            values = Collections.nCopies(keys.size(), null);
        }

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

                //응답 저장 후 redis 적재
                result.put(commentId, count);
                safeSet(COMMENT_LIKE_KEY + commentId, String.valueOf(count));
            });
        }
        return result;
    }

    public void syncViewCountsToDB() {
        Set<String> dirtyIds = getDirtySetAndSwap(DIRTY_VIEWCOUNT_KEY);

        if (dirtyIds.isEmpty()) {
            return; //이번 주기 배치 없음
        }

        try {
            //배치 대상 키 리스트
            List<String> keys = dirtyIds.stream()
                    .map(id -> POST_VIEWCOUNT_KEY + id)
                    .toList();

            //배치 대상 키의 value MGET으로 가져옴
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                values = Collections.emptyList();
            }

            //배치 대상 id, value를 돌면서 countMap에 저장
            Map<Long, Integer> countMap = new HashMap<>();
            Iterator<String> idIt = dirtyIds.iterator();
            Iterator<String> valIt = values.iterator();
            while (idIt.hasNext() && valIt.hasNext()) {
                Long id = Long.valueOf(idIt.next());
                String val = valIt.next();
                if (val != null) {
                    countMap.put(id, Integer.valueOf(val));
                }
            }

            if (!countMap.isEmpty()) {
                postBatchRepository.batchUpdateViewCount(countMap);
            }
            commitDirtySet(DIRTY_VIEWCOUNT_KEY);    //트랜잭션이 완료되면 삭제한 채로 두고
        } catch (Exception e) {
            rollbackDirtySet(DIRTY_VIEWCOUNT_KEY, dirtyIds, e);    //실패 시 롤백 됨 롤백도 실패하면 로그 남김(파라미터로 원본 예외 객체 넣음)
            throw e;    //다시 해당 예외 던짐
        }

    }

    public void syncRecommendCountsToDB() {
        Set<String> dirtyIds = getDirtySetAndSwap(DIRTY_RECOMMEND_KEY);

        if (dirtyIds.isEmpty()) {
            return;
        }

        try {
            //배치 대상 키 리스트
            List<String> keys = dirtyIds.stream()
                    .map(id -> POST_RECOMMEND_KEY + id)
                    .toList();

            //배치 대상 키의 value MGET으로 가져옴
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                values = Collections.emptyList();
            }

            //배치 대상 id, value를 돌면서 countMap에 저장
            Map<Long, Integer> countMap = new HashMap<>();
            Iterator<String> idIt = dirtyIds.iterator();
            Iterator<String> valIt = values.iterator();
            while (idIt.hasNext() && valIt.hasNext()) {
                Long id = Long.valueOf(idIt.next());
                String val = valIt.next();
                if (val != null) {
                    countMap.put(id, Integer.valueOf(val));
                }
            }

            if (!countMap.isEmpty()) {
                postBatchRepository.batchUpdateRecommendCount(countMap);
            }
            commitDirtySet(DIRTY_RECOMMEND_KEY);
        } catch (Exception e) {
            rollbackDirtySet(DIRTY_RECOMMEND_KEY, dirtyIds, e);
            throw e;
        }

    }


    public void syncDisrecommendCountsToDB() {
        Set<String> dirtyIds = getDirtySetAndSwap(DIRTY_DISRECOMMEND_KEY);

        if (dirtyIds.isEmpty()) {
            return;
        }

        try {
            //배치 대상 키 리스트
            List<String> keys = dirtyIds.stream()
                    .map(id -> POST_DISRECOMMEND_KEY + id)
                    .toList();

            //배치 대상 키의 value MGET으로 가져옴
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                values = Collections.emptyList();
            }

            //배치 대상 id, value를 돌면서 countMap에 저장
            Map<Long, Integer> countMap = new HashMap<>();
            Iterator<String> idIt = dirtyIds.iterator();
            Iterator<String> valIt = values.iterator();
            while (idIt.hasNext() && valIt.hasNext()) {
                Long id = Long.valueOf(idIt.next());
                String val = valIt.next();
                if (val != null) {
                    countMap.put(id, Integer.valueOf(val));
                }
            }

            if (!countMap.isEmpty()) {
                postBatchRepository.batchUpdateDisrecommendCount(countMap);
            }
            commitDirtySet(DIRTY_DISRECOMMEND_KEY);
        } catch (Exception e) {
            rollbackDirtySet(DIRTY_DISRECOMMEND_KEY, dirtyIds, e);
            throw e;
        }


    }

    public void syncCommentLikeCountsToDB() {
        Set<String> dirtyIds = getDirtySetAndSwap(DIRTY_COMMENT_LIKE_KEY);

        if (dirtyIds.isEmpty()) {
            return;
        }

        try {
            //배치 대상 키 리스트
            List<String> keys = dirtyIds.stream()
                    .map(id -> COMMENT_LIKE_KEY + id)
                    .toList();

            //배치 대상 키의 value MGET으로 가져옴
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                values = Collections.emptyList();
            }

            //배치 대상 id, value를 돌면서 countMap에 저장
            Map<Long, Integer> countMap = new HashMap<>();
            Iterator<String> idIt = dirtyIds.iterator();
            Iterator<String> valIt = values.iterator();
            while (idIt.hasNext() && valIt.hasNext()) {
                Long id = Long.valueOf(idIt.next());
                String val = valIt.next();
                if (val != null) {
                    countMap.put(id, Integer.valueOf(val));
                }
            }

            if (!countMap.isEmpty()) {
                commentBatchRepository.updateCommentLike(countMap);
            }
            commitDirtySet(DIRTY_COMMENT_LIKE_KEY);
        } catch (Exception e) {
            rollbackDirtySet(DIRTY_COMMENT_LIKE_KEY, dirtyIds, e);
            throw e;
        }

    }


    private Set<String> getDirtySetAndSwap(String dirtyKey) {
        //:processing으로 이름 변경 (배치 도중 들어오는 요청이 무시되지 않게)
        String processingKey = dirtyKey + ":processing";
        try {
            redisTemplate.rename(dirtyKey, processingKey);
        } catch (DataAccessException e) {   //rename할게 없는 경우, redis 장애의 경우
            // dirtyKey가 없음 = 이번 주기에 변동 없음
            return Collections.emptySet();
        }

        Set<String> members = redisTemplate.opsForSet().members(processingKey);
        //dirty set을 여기서 삭제하지 않고 sync에게 책임을 넘김
        return members != null ? members : Collections.emptySet();
    }

    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {   //redis 장애 등
            return null;
        }
    }

    private List<String> safeMultiGet(List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (DataAccessException e) {
            return null; //장애 시 전부 미스 처리하도록 null로 통일
        }
    }

    private void safeSet(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (DataAccessException e) {
            //캐싱 실패는 무시 응답엔 이미 DB값이 담겨있고, Redis 복구되면 다음 요청에서 다시 채워짐
        }
    }

    private void commitDirtySet(String dirtyKey) {
        redisTemplate.delete(dirtyKey + ":processing");
    }

    private void rollbackDirtySet(String dirtyKey, Set<String> members, Exception originalException) {
        if (members.isEmpty()) return;
        try {
            redisTemplate.opsForSet().add(dirtyKey, members.toArray(new String[0]));
            redisTemplate.delete(dirtyKey + ":processing");
        } catch (DataAccessException rollbackFailure) {
            originalException.addSuppressed(rollbackFailure);
            log.error("dirty set 롤백 실패, 데이터 유실 가능성 있음. dirtyKey={}, members={}", dirtyKey, members, rollbackFailure);
        }

    }

}

