package com.kangyoon.community.domain.notification.controller;

import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import com.kangyoon.community.domain.notification.service.NotificationService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Tag(name = "Notification", description = "Notification API")
    @Operation(
            summary = "SSE 구독",
            description = "SSE 연결을 위해 서버를 구독합니다."
    )
    @GetMapping(value = "/api/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        Long userId = userDetails.getMemberId();
        return notificationService.subscribe(userId, lastEventId);
    }

    @Tag(name = "Notification", description = "Notification API")
    @Operation(
            summary = "알림 목록",
            description = "SSE 알림 목록을 조회합니다.(최대 30개)"
    )
    @GetMapping("/api/notifications/notice")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> noticeList(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getMemberId();
        List<NotificationResponse> noticeList = notificationService.noticeList(userId);

        return ResponseEntity.ok(new ApiResponse<>("알림 목록 조회 성공", noticeList));
    }

    @Tag(name = "Notification", description = "Notification API")
    @Operation(
            summary = "알림 읽음처리",
            description = "SSE 알림 목록의 알림을 읽음처리합니다."
    )
    @PatchMapping("/api/notifications/notice/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> noticeRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.readNotice(userDetails.getMemberId(), notificationId);
        return ResponseEntity.ok(new ApiResponse<>("알림 읽음처리 완료", null));
    }

    @Tag(name = "Notification", description = "Notification API")
    @Operation(
            summary = "알림 삭제",
            description = "SSE 알림 목록의 알림을 삭제합니다."
    )
    @DeleteMapping("/api/notifications/notice/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> noticeDelete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        notificationService.deleteNotice(userDetails.getMemberId(), notificationId);
        return ResponseEntity.ok(new ApiResponse<>("알림 삭제 완료", null));
    }
}
