package com.everypoll.pollService.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.everypoll.common.event.auth.UserDeletedEvent;
import com.everypoll.common.event.vote.VoteCancelledEvent;
import com.everypoll.common.event.vote.VoteCreatedEvent;
import com.everypoll.pollService.config.KafkaConfig;
import com.everypoll.pollService.service.PollService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollEventConsumer {
    private final PollService pollService;

    @KafkaListener(
        topics = KafkaConfig.USER_EVENTS_TOPIC,
        groupId = "poll-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if(event instanceof UserDeletedEvent userDeletedEvent) {
            log.info("유저 삭제 이벤트 응답받음! - userId: {}", userDeletedEvent.getUserId());
            
            try {
                pollService.handleUserDeleted(userDeletedEvent.getUserId());
                log.info("유저 삭제 이벤트 처리됨! - userId: {}", userDeletedEvent.getUserId());
            } catch (Exception e) {
                log.error("유저 삭제 이벤트 실패. - userId: {}", userDeletedEvent.getUserId(), e);
            }
        }
    }

    @KafkaListener(
            topics = KafkaConfig.VOTE_EVENTS_TOPIC,
            groupId = "poll-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleVoteEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof VoteCreatedEvent voteCreatedEvent) {
            log.info("투표 이벤트 응답받음! - pollId: {}, optionId: {}",
                    voteCreatedEvent.getPollId(), voteCreatedEvent.getOptionId());
            
            try {
                pollService.incrementVoteCount(
                        voteCreatedEvent.getPollId(),
                        voteCreatedEvent.getOptionId()
                );
                log.info("투표 숫자 증가 성공! - pollId: {}, optionId: {}",
                        voteCreatedEvent.getPollId(), voteCreatedEvent.getOptionId());
            } catch (Exception e) {
                log.error("투표 숫자 증가 실패! - pollId: {}, optionId: {}, error: {}", 
                    voteCreatedEvent.getPollId(), voteCreatedEvent.getOptionId(), e.getMessage(), e);
                throw e;
            }
        } else if (event instanceof VoteCancelledEvent voteCancelledEvent) {
            log.info("투표 취소 이벤트 응답받음! - pollId: {}, optionId: {}",
                    voteCancelledEvent.getPollId(), voteCancelledEvent.getOptionId());
            
            try {
                pollService.decrementVoteCount(
                        voteCancelledEvent.getPollId(),
                        voteCancelledEvent.getOptionId()
                );
                log.info("투표 숫자 감소 성공! - pollId: {}, optionId: {}",
                        voteCancelledEvent.getPollId(), voteCancelledEvent.getOptionId());
            } catch (Exception e) {
                log.error("투표 숫자 증가 실패! - pollId: {}, optionId: {}, error: {}", 
                    voteCancelledEvent.getPollId(), voteCancelledEvent.getOptionId(), e.getMessage(), e);
                
                throw e;
            }
        }
    }
}
