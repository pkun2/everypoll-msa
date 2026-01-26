package com.everypoll.authService.event;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.everypoll.authService.config.KafkaConfig;
import com.everypoll.common.event.auth.UserCreatedEvent;
import com.everypoll.common.event.auth.UserDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCreated(Long userId, String username, String email, List<String> roles) {
        UserCreatedEvent event = UserCreatedEvent.of(userId, username, email, roles);
        publish(event, String.valueOf(userId));
    }

    public void publishUserDeleted(Long userId, String username) {
        UserDeletedEvent event = UserDeletedEvent.of(userId, username);
        publish(event, String.valueOf(userId));
    }

    private void publish(Object event, String key) {
        CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(KafkaConfig.USER_EVENTS_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("이벤트 생성됨! - topic: {}, key: {}, event: {}",
                        KafkaConfig.USER_EVENTS_TOPIC, key, event.getClass().getSimpleName());
            } else {
                log.error("이벤트 생성 실패. - topic: {}, key: {}, error: {}",
                        KafkaConfig.USER_EVENTS_TOPIC, key, ex.getMessage());
            }
        });
    }
}