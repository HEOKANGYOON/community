package com.kangyoon.community.domain.notification.service;

import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import com.kangyoon.community.domain.notification.entity.Notification;
import com.kangyoon.community.domain.notification.repository.NotificationRepository;
import com.kangyoon.community.domain.notification.repository.SseEmitterRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationRepository notificationRepository;

    public SseEmitter subscribe(Long userId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);   //30분짜리 emitter 생성
        sseEmitterRepository.save(userId, emitter);

        if (lastEventId != null) {
            Long notificationId = Long.parseLong(lastEventId);
            List<Notification> missedNotice = notificationRepository.findByMemberIdAndIdGreaterThanAndDeletedAtIsNull(userId, notificationId);

            missedNotice.forEach(notification -> {
                try {
                    emitter.send((SseEmitter.event()
                            .id(String.valueOf(notification.getId()))
                            .data(NotificationResponse.from(notification))));
                } catch (IOException e) {
                    sseEmitterRepository.deleteByUserId(userId);
                }
            });
        }
        emitter.onCompletion(() -> sseEmitterRepository.deleteByUserId(userId));
        emitter.onTimeout(() -> sseEmitterRepository.deleteByUserId(userId));

        return emitter;
    }

    public List<NotificationResponse> noticeList(Long userId) {
        List<Notification> notificationList = notificationRepository.findTop30ByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        return notificationList.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public void readNotice(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndMemberIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notification.notificationRead();

    }

    public void deleteNotice(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndMemberIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTICE_NOT_FOUND));

        notification.notificationDelete();
    }

}