package com.everypoll.pollService.event;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.everypoll.common.event.poll.CommentCreatedEvent;
import com.everypoll.common.event.poll.PollCreatedEvent;
import com.everypoll.common.event.poll.PollDeletedEvent;
import com.everypoll.pollService.config.KafkaConfig;
import com.everypoll.pollService.model.Comment;
import com.everypoll.pollService.model.Poll;
import java.time.LocalDateTime;

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
                        .displayOrder(opt.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        PollCreatedEvent event = PollCreatedEvent.of(
                poll.getId(),
                poll.getTitle(),
                poll.getDescription(),
                poll.getEndAt().toString(),
                poll.getCreatedBy(),
                options);

        publish(event, String.valueOf(poll.getId()));
    }

    public void publishPollDeleted(Long pollId, String deletedBy) {
        PollDeletedEvent event = PollDeletedEvent.of(pollId, deletedBy);
        publish(event, String.valueOf(pollId));
    }

    public void publishCommentCreated(Comment comment) {
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .type("COMMENT_CREATED")
                .commentId(comment.getId())
                .pollId(comment.getPoll().getId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        publish(event, String.valueOf(comment.getPoll().getId()));
    }

    private void publish(Object event, String key) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(KafkaConfig.POLL_EVENTS_TOPIC, key,
                event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Kafka 이벤트 발행 성공! - key: {}, event: {}",
                        key, event.getClass().getSimpleName());
            } else {
                log.error("Kafka 이벤트 발행 실패. - key: {}, error: {}",
                        key, ex.getMessage());
            }
        });
    }
}
