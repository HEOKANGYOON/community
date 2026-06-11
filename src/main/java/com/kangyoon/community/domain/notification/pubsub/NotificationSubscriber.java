package com.kangyoon.community.domain.notification.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import com.kangyoon.community.domain.notification.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class NotificationSubscriber {

    private final SseEmitterRepository sseEmitterRepository;
    private final ObjectMapper objectMapper;

    public void onMessage(String message, String channel) {
        try {
            Long userId = Long.parseLong(channel.replace("notification:", ""));
            NotificationResponse response = objectMapper.readValue(message, NotificationResponse.class);

            sseEmitterRepository.findByUserId(userId).ifPresent(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(response.id()))
                            .data(response));
                } catch (IOException e) {
                    sseEmitterRepository.deleteByUserId(userId);
                }
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
