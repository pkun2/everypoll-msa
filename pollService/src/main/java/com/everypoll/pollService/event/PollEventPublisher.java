package com.everypoll.pollService.event;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.everypoll.common.event.poll.PollCreatedEvent;
import com.everypoll.common.event.poll.PollDeletedEvent;
import com.everypoll.pollService.config.KafkaConfig;
import com.everypoll.pollService.model.Poll;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PollEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPollCreated(Poll poll) {
        List<PollCreatedEvent.OptionInfo> options = poll.getOptions().stream()
            .map(opt -> PollCreatedEvent.OptionInfo.builder()
                .optionId(opt.getId())
                .optionText(opt.getOptionText())
                .build())
            .collect(Collectors.toList());
        
        PollCreatedEvent event = PollCreatedEvent.of(
            poll.getId(), 
            poll.getQuestion(),
            poll.getCreatedBy(),
            options
        );

        publish(event, String.valueOf(poll.getId()));
    }

    public void publishPollDeleted(Long pollId, String deletedBy) {
        PollDeletedEvent event = PollDeletedEvent.of(pollId, deletedBy);
        publish(event, String.valueOf(pollId));
    }

    private void publish(Object event, String key) {
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(KafkaConfig.POLL_EVENTS_TOPIC, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("투표 게시글 이벤트 생성됨! - key: {}, event: {}",
                        key, event.getClass().getSimpleName());
            } else {
                log.error("투표 게시글 이벤트 생성 실패. - key: {}, error: {}",
                        key, ex.getMessage());
            }
        });
    }
}
