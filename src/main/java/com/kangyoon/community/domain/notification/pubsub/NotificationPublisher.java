package com.kangyoon.community.domain.notification.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, NotificationResponse response) {
        try {
            String message = objectMapper.writeValueAsString(response);
            redisTemplate.convertAndSend("notification:" + userId, message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
