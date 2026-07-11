package com.kangyoon.community.infrastructure.redis;

import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.repository.CommentLikeRepository;
import com.kangyoon.community.domain.comment.repository.CommentRepository;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.VoteType;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.domain.post.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWarmupRunner implements ApplicationRunner {

    private static final String POST_RECOMMEND_KEY = "post:recommend:";
    private static final String POST_DISRECOMMEND_KEY = "post:disrecommend:";
    private static final String POST_VIEWCOUNT_KEY = "post:viewcount:";
    private static final String COMMENT_LIKE_KEY = "comment:like:";

    private static final int PAGE_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostVoteRepository postVoteRepository;


    @Override
    public void run(ApplicationArguments args) {
        try {
            warmupPosts();
            warmupComments();
        } catch (DataAccessException e) {
            // 웜업 시점에 Redis가 안 떠있는 경우: 서버는 정상 기동시키고
            // 이후엔 기존 개별 캐시미스 fallback(safeGet/safeSet)이 처리하도록 넘긴다
            log.warn("Redis 웜업 실패, 개별 요청의 캐시미스 fallback으로 대체됩니다.", e);
        }
    }

    private void warmupPosts() {
        int pageNumber = 0;
        Page<Post> page;

        do {
            page = postRepository.findAllByDeletedAtIsNull(PageRequest.of(pageNumber, PAGE_SIZE));
            List<Long> postIds = page.getContent().stream().map(Post::getId).toList();

            Map<Long, Integer> recommendCounts = postVoteRepository.countByPostIdsAndVoteType(postIds, VoteType.UP);
            Map<Long, Integer> disrecommendCounts = postVoteRepository.countByPostIdsAndVoteType(postIds, VoteType.DOWN);

            Map<String, String> batch = new HashMap<>();

            page.getContent().forEach(post -> {
                // viewCount는 유일한 소스가 스냅샷이므로 그대로 사용
                batch.put(POST_VIEWCOUNT_KEY + post.getId(), String.valueOf(post.getViewCount()));
                batch.put(POST_RECOMMEND_KEY + post.getId(), String.valueOf(recommendCounts.getOrDefault(post.getId(), 0)));
                batch.put(POST_DISRECOMMEND_KEY + post.getId(), String.valueOf(disrecommendCounts.getOrDefault(post.getId(), 0)));
            });

                redisTemplate.opsForValue().multiSet(batch);

            pageNumber++;
        } while (page.hasNext());

        log.info("게시글 캐시 웜업 완료");
    }

    private void warmupComments() {
        int pageNumber = 0;
        Page<Comment> page;

        do {
            page = commentRepository.findAllByDeletedAtIsNull(PageRequest.of(pageNumber, PAGE_SIZE));
            List<Long> commentIds = page.getContent().stream().map(Comment::getId).toList();

            Map<Long, Integer> likeCounts = commentLikeRepository.countByCommentIds(commentIds);

            Map<String, String> batch = commentIds.stream()
                    .collect(Collectors.toMap(
                            id -> COMMENT_LIKE_KEY + id,
                            id -> String.valueOf(likeCounts.getOrDefault(id, 0))
                    ));


            redisTemplate.opsForValue().multiSet(batch);

            pageNumber++;
        } while (page.hasNext());

        log.info("댓글 캐시 웜업 완료");
    }

}
