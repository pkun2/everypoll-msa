package com.everypoll.pollService.controller;

import com.everypoll.pollService.dto.CommentCreateRequest;
import com.everypoll.pollService.dto.CommentResponse;
import com.everypoll.pollService.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());

        CommentResponse response = commentService.createComment(pollId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Slice<CommentResponse>> getComments( // 댓글 불러오기
            @PathVariable Long pollId,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        Long currentUserId = (userDetails != null && userDetails.getUsername() != null
                && !userDetails.getUsername().equals("anonymousUser"))
                        ? Long.valueOf(userDetails.getUsername())
                        : null;
        Slice<CommentResponse> responses = commentService.getCommentsByPollId(pollId,
                currentUserId, pageable);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment( // 댓글 삭제
            @PathVariable Long pollId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(userDetails.getUsername());
        commentService.deleteComment(pollId, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
