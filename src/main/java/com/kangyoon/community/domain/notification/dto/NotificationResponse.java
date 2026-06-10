package com.kangyoon.community.domain.notification.dto;

import com.kangyoon.community.domain.notification.entity.Notification;
import com.kangyoon.community.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType notificationType,
        String content,
        Long targetId,
        boolean isRead,
        LocalDateTime createAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getContent(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
