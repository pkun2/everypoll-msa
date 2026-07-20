package com.everypoll.pollService.controller;

import com.everypoll.pollService.dto.CommentCreateRequest;
import com.everypoll.pollService.dto.CommentResponse;
import com.everypoll.pollService.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@RestController
@RequestMapping("/api/polls/{pollId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> createComment( // 댓글 생성
            @PathVariable Long pollId,
            @Valid @RequestBody CommentCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        CommentResponse response = commentService.createComment(pollId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Slice<CommentResponse>> getComments( // 댓글 불러오기
            @PathVariable Long pollId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            Pageable pageable) {
        Slice<CommentResponse> responses = commentService.getCommentsByPollId(pollId,
                currentUserId, pageable);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment( // 댓글 삭제
            @PathVariable Long pollId,
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId) {

        commentService.deleteComment(pollId, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
