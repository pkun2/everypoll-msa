package com.everypoll.voteService.event;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.everypoll.common.event.vote.VoteCancelledEvent;
import com.everypoll.common.event.vote.VoteCreatedEvent;
import com.everypoll.voteService.config.KafkaConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishVoteCreated(Long voteId, Long pollId, Long optionId, Long userId) {
        VoteCreatedEvent event = VoteCreatedEvent.of(voteId, pollId, optionId, userId);
        publish(event, String.valueOf(pollId));
    }

    public void publishVoteCancelled(Long voteId, Long pollId, Long optionId, Long userId) {
        VoteCancelledEvent event = VoteCancelledEvent.of(voteId, pollId, optionId, userId);
        publish(event, String.valueOf(pollId));
    }

    private void publish(Object event, String key) {
        CompletableFuture<SendResult<String, Object>> future = 
               kafkaTemplate.send(KafkaConfig.VOTE_EVENTS_TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("이벤트 생성됨! - topic: {}, key: {}, event: {}",
                        KafkaConfig.VOTE_EVENTS_TOPIC, key, event.getClass().getSimpleName());
            } else {
                log.error("이벤트 생성 실패. - topic: {}, key: {}, error: {}",
                        KafkaConfig.VOTE_EVENTS_TOPIC, key, ex.getMessage());
            }
        });
    }
}
