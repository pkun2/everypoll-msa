package com.everypoll.authService.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.everypoll.common.dto.CreatedUserEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, CreatedUserEvent> kafkaTemplate;
    private static final String TOPIC_NAME = "user-creation-topic";

    public void sendUserCreationEvent(CreatedUserEvent event) {
      log.info("사용자 생성 이벤트 발생!: userId={}", event.getUserId());
      // kafkaTemplate.send(TOPIC_NAME, event);

      CompletableFuture<SendResult<String, CreatedUserEvent>> future = kafkaTemplate.send(TOPIC_NAME, event.getUserId(), event);

      future.whenComplete((result, ex) -> {
          if (ex != null) {
              // 전송 실패 시
              log.error("Failed to send message for userId={}: {}", event.getUserId(), ex.getMessage());
          } else {
              // 전송 성공 시
              log.info("Successfully sent message for userId={} with offset: {}",
                      event.getUserId(), result.getRecordMetadata().offset());
          }
      });
	}
}
