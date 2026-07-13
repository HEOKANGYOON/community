package com.kangyoon.community.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSyncScheduler {

    private final RedisService redisService;

    //5분마다 redis에 적재된 것 post -> 조회수, 추천수, 비추천수 comment -> 댓글 좋아요 수 배치
    @Scheduled(fixedDelay = 300000)
    public void sync() {
        safeSync(redisService::syncViewCountsToDB, "조회수");
        safeSync(redisService::syncRecommendCountsToDB, "추천수");
        safeSync(redisService::syncDisrecommendCountsToDB, "비추천수");
        safeSync(redisService::syncCommentLikeCountsToDB, "댓글좋아요");
    }

    //하나가 실패해도 다음 배치가 실행 될 수 있도록
    private void safeSync(Runnable task, String label) {
        try {
            task.run();
        } catch (Exception e) { //DataAccessException → Exception   예상 외의 런타임 잡기(NumberFormatException 등)
            log.warn("{} 배치 동기화 실패", label, e);
        }
    }
}
