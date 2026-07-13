package com.kangyoon.community.domain.post.event;

import com.kangyoon.community.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostImageMoveEventListener {

    private final S3Service s3Service;
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 500;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostImageMoveEvent event) {
        event.tempUrls().forEach(this::moveWithRetry);
    }

    private void moveWithRetry(String tempUrl) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                s3Service.moveToPostFolder(tempUrl); // 여기서 실제 copy+delete
                return;
            } catch (Exception e) {
                log.warn("이미지 이동 실패 (시도 {}/{}): {}", attempt, MAX_RETRY, tempUrl, e);
                if (attempt == MAX_RETRY) {
                    log.error("이미지 이동 최종 실패, 수동 확인 필요: {}", tempUrl, e);
                    return;
                }
                sleep(RETRY_DELAY_MS * attempt);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
