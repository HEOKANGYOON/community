package com.kangyoon.community.domain.notification.repository;

import com.kangyoon.community.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdAndIdGreaterThanAndDeletedAtIsNull(Long memberId, Long notificationId);
    List<Notification> findTop30ByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId);
    Optional<Notification> findByIdAndMemberIdAndDeletedAtIsNull(Long notificationId, Long memberId)
}
