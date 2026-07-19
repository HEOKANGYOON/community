package com.kangyoon.community.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStartupFlushRunner implements ApplicationRunner {

    private final RedisService redisService;

    @Override
    public void run(ApplicationArguments args) {
        //서버 시작 전 변돌사항이 배치되지 못한 것 적용
        safeFlush(redisService::flushViewDeltaToDb, "조회수");   // 변경: syncViewCountsToDB → flushViewDeltaToDb
        safeFlush(redisService::flushRecommendDeltaToDb, "추천수");        // 변경
        safeFlush(redisService::flushDisrecommendDeltaToDb, "비추천수");    // 변경
        safeFlush(redisService::flushCommentLikeDeltaToDb, "댓글좋아요");    // 변경
    }

    private void safeFlush(Runnable task, String label) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("웜업 전 {} 잔여분 반영 실패, DB 스냅샷 기준으로 진행", label, e);
        }
    }

}
