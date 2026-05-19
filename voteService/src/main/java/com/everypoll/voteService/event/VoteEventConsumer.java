package com.everypoll.voteService.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.everypoll.common.event.auth.UserDeletedEvent;
import com.everypoll.common.event.poll.PollDeletedEvent;
import com.everypoll.voteService.config.KafkaConfig;
import com.everypoll.voteService.service.VoteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventConsumer {
    private final VoteService voteService;

    @KafkaListener(
            topics = KafkaConfig.USER_EVENTS_TOPIC,
            groupId = "vote-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof UserDeletedEvent userDeletedEvent) {
            log.info("사용자 삭제 이벤트 응답받음 - userId: {}", userDeletedEvent.getUserId());
            
            try {
                voteService.handleUserDeleted(userDeletedEvent.getUserId());
                log.info("사용자 삭제 이벤트 성공! - userId: {}", userDeletedEvent.getUserId());
            } catch (Exception e) {
                log.error("사용자 삭제 이벤트 실패. - error: {}", e.getMessage());
            }
        }
    }

    @KafkaListener(
            topics = KafkaConfig.POLL_EVENTS_TOPIC,
            groupId = "vote-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePollEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof PollDeletedEvent pollDeletedEvent) {
            log.info("투표 게시글 삭제 이벤트 응답 받음 - pollId: {}", pollDeletedEvent.getPollId());
            
            try {
                voteService.handlePollDeleted(pollDeletedEvent.getPollId());
                log.info("투표 게시글 삭제 성공! - pollId: {}", pollDeletedEvent.getPollId());
            } catch (Exception e) {
                log.error("투표 게시글 삭제 실패. - error: {}", e.getMessage());
            }
        }
    }
}
