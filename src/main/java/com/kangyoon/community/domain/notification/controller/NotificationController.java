package com.kangyoon.community.domain.notification.controller;

import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import com.kangyoon.community.domain.notification.service.NotificationService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/api/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        Long userId = userDetails.getMemberId();
        return notificationService.subscribe(userId, lastEventId);
    }

    @GetMapping("/api/notifications/notice")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> noticeList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getMemberId();
        List<NotificationResponse> noticeList = notificationService.noticeList(userId);

        return ResponseEntity.ok(new ApiResponse<>("알림 목록 조회 성공", noticeList));
    }

    @PatchMapping("/api/notifications/notice/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> noticeRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.readNotice(userDetails.getMemberId(), notificationId);
        return ResponseEntity.ok(new ApiResponse<>("알림 읽음처리 완료", null));
    }

    @DeleteMapping("/api/notifications/notice/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> noticeDelete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotice(userDetails.getMemberId(), notificationId);
        return ResponseEntity.ok(new ApiResponse<>("알림 삭제 완료", null));
    }
}
