package com.kangyoon.community.domain.notification.entity;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Notification extends BaseTimeEntity {

    @Builder(access = AccessLevel.PRIVATE)
    private Notification(Member member, NotificationType notificationType, NotificationTargetType targetType, Long targetId) {
        this.member = member;
        this.notificationType = notificationType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.content = notificationType.toContent();
        this.isRead = false;
    }

    public static Notification from(Member member, NotificationType notificationType, NotificationTargetType targetType, Long targetId) {
        return Notification.builder()
                .member(member)
                .notificationType(notificationType)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private NotificationType notificationType;
    @Column(nullable = false) @Enumerated(EnumType.STRING) private NotificationTargetType targetType;
    @Column(nullable = false) private Long targetId;
    @Column(nullable = false) private String content;
    @Column(nullable = false) private boolean isRead;
    private LocalDateTime deletedAt;


    public void notificationRead() {
        this.isRead = true;
    }

    public void notificationDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}
