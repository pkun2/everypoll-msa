package com.everypoll.pollService.service;

import com.everypoll.common.event.poll.CommentCreatedEvent;
import com.everypoll.pollService.config.KafkaConfig;
import com.everypoll.pollService.dto.CommentCreateRequest;
import com.everypoll.pollService.dto.CommentResponse;
import com.everypoll.pollService.exception.ResourceNotFoundException;
import com.everypoll.pollService.model.Comment;
import com.everypoll.pollService.model.Poll;
import com.everypoll.pollService.repository.CommentRepository;
import com.everypoll.pollService.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PollRepository pollRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.everypoll.pollService.client.AuthServiceClient authServiceClient;

    @Transactional
    public CommentResponse createComment(Long pollId, Long userId, CommentCreateRequest request) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        Comment comment = Comment.builder()
                .poll(poll)
                .userId(userId)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        publishCommentCreatedEvent(savedComment);

        return CommentResponse.of(savedComment, "User" + userId, userId);
    }

    @Transactional(readOnly = true)
    public Slice<CommentResponse> getCommentsByPollId(Long pollId, Long currentUserId, Pageable pageable) {
        Slice<Comment> comments = commentRepository.findByPollId(pollId, pageable);

        if (!pollRepository.existsById(pollId)) {
            throw new ResourceNotFoundException("Poll", "id", pollId);
        }

        // 1. 코멘트들로부터 userId Set을 추출합니다.
        java.util.Set<Long> userIds = comments.getContent().stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());

        // 2. AuthServiceClient를 통해 사용자 이름 매핑을 가져옵니다.
        java.util.Map<Long, String> usernameMap = java.util.Collections.emptyMap();
        if (!userIds.isEmpty()) {
            try {
                usernameMap = authServiceClient.getUsersNames(userIds);
            } catch (Exception e) {
                log.warn("AuthService 통신 실패 또는 에러 발생: {}", e.getMessage());
            }
        }

        final java.util.Map<Long, String> finalUsernameMap = usernameMap;

        // 3. Slice.map()을 통해 응답 객체로 변환합니다.
        return comments.map(comment -> CommentResponse.of(
                comment,
                finalUsernameMap.getOrDefault(comment.getUserId(), "알 수 없는 사용자"),
                currentUserId != null ? currentUserId : -1L));
    }

    @Transactional
    public void deleteComment(Long pollId, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getPoll().getId().equals(pollId)) {
            throw new IllegalArgumentException("댓글이 해당 투표에 속하지 않습니다.");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("댓글을 삭제할 권한이 없습니다.");
        }

        comment.delete();
    }

    private void publishCommentCreatedEvent(Comment comment) {
        CommentCreatedEvent event = CommentCreatedEvent.builder()
                .type("commentCreated")
                .commentId(comment.getId())
                .pollId(comment.getPoll().getId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaConfig.POLL_EVENTS_TOPIC, String.valueOf(comment.getPoll().getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("댓글 이벤트 발행 성공! - commentId: {}", comment.getId());
                    } else {
                        log.error("댓글 이벤트 발행 실패! - commentId: {}, error: {}", comment.getId(), ex.getMessage());
                    }
                });
    }
}
