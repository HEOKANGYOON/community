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
public class PostImageCleanupListener {

    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostImageCleanupEvent event) {
        event.imageUrls().forEach(url -> {
                    try {
                        s3Service.deleteObject(url);
                    } catch (Exception e) {
                        //개별 이미지 삭제 실패시 로그만 남기고 다음 실행
                        log.error("고아 이미지 삭제 실패: {}", url, e);
                    }
                });
    }
}
