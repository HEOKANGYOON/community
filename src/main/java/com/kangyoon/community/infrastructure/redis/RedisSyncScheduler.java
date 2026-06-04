package com.kangyoon.community.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSyncScheduler {

    private final RedisService redisService;

    //5분마다 redis에 적재된 것 post -> 조회수, 추천수, 비추천수 comment -> 댓글 좋아요 수 배치
    @Scheduled(fixedDelay = 300000)
    public void sync() {
        redisService.syncViewCountsToDB();
        redisService.syncRecommendCountsToDB();
        redisService.syncDisrecommendCountsToDB();
        redisService.syncCommentLikeCountsToDB();
    }

}
