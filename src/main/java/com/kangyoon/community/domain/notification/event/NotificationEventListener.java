package com.kangyoon.community.domain.notification.event;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import com.kangyoon.community.domain.notification.entity.Notification;
import com.kangyoon.community.domain.notification.pubsub.NotificationPublisher;
import com.kangyoon.community.domain.notification.repository.NotificationRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationPublisher notificationPublisher;

    //Propagation.REQUIRES_NEW 옵션을 주는 이유는 무조건 새 트랜잭션을 열어서 댓글작성, 대댓글, 댓글 좋아요 등의 작업이 알림 저장 실패 때문에 롤백 되는것을 방지하기 위함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        Member member = memberRepository.findById(event.receiverId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Notification notification = Notification.from(
                member,
                event.notificationType(),
                event.notificationTargetType(),
                event.targetId()
        );
        notificationRepository.save(notification);

        notificationPublisher.publish(event.receiverId(),
                NotificationResponse.from(notification));
    }
}
