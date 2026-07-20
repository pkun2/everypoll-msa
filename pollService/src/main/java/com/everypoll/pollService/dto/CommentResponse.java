package com.everypoll.pollService.dto;

import java.time.LocalDateTime;

import com.everypoll.pollService.model.Comment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통해서만 생성하도록 강제
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private boolean isOwner;

    public static CommentResponse from(Comment comment, String username, Long currentUserId) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUserId())
                .username(username)
                .createdAt(comment.getCreatedAt())
                .isOwner(comment.getUserId().equals(currentUserId))
                .build();
    }
}