package com.kangyoon.community.infrastructure.redis;

import com.kangyoon.community.domain.comment.repository.CommentBatchRepository;
import com.kangyoon.community.domain.comment.repository.CommentLikeRepository;
import com.kangyoon.community.domain.post.repository.PostBatchRepository;
import com.kangyoon.community.domain.post.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconciliationScheduler {

    private final PostVoteRepository postVoteRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostBatchRepository postBatchRepository;
    private final CommentBatchRepository commentBatchRepository;
    private final RedisService redisService;

    @Scheduled(cron = "0 0 4 * * *")  // 매일 새벽 4시
    public void reconcile() {
        // 재계산 전, Redis에 남아있는 delta를 먼저 DB에 반영 (부풀림 방지)
        safeRun(redisService::flushRecommendDeltaToDb, "추천 사전 flush");
        safeRun(redisService::flushDisrecommendDeltaToDb, "비추천 사전 flush");
        safeRun(redisService::flushCommentLikeDeltaToDb, "댓글좋아요 사전 flush");

        LocalDateTime since = LocalDateTime.now().minusHours(25);

        safeRun(() -> {
            List<Long> postIds = postVoteRepository.findDistinctPostIdsSince(since);
            postBatchRepository.reconcileRecommendCounts(postIds);
            postBatchRepository.reconcileDisrecommendCounts(postIds);
            log.info("추천/비추천 reconciliation 완료, 대상 postId 수={}", postIds.size());
        }, "추천/비추천 reconciliation");

        safeRun(() -> {
            List<Long> commentIds = commentLikeRepository.findDistinctCommentIdsSince(since);
            commentBatchRepository.reconcileCommentLikeCounts(commentIds);
            log.info("댓글좋아요 reconciliation 완료, 대상 commentId 수={}", commentIds.size());
        }, "댓글좋아요 reconciliation");
    }

    private void safeRun(Runnable task, String label) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("{} 실패", label, e);
        }
    }

}
