package com.kangyoon.community.domain.notification.event;

import com.kangyoon.community.domain.notification.entity.NotificationTargetType;
import com.kangyoon.community.domain.notification.entity.NotificationType;

public record NotificationEvent(
        Long receiverId,
        NotificationType notificationType,
        NotificationTargetType notificationTargetType,
        Long targetId
) { }
