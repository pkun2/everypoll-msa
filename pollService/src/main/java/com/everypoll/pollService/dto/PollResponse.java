package com.everypoll.pollService.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.everypoll.pollService.model.Poll;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder 
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통해서만 생성하도록 강제
public class PollResponse {
    private Long id;
    private String question;
    private LocalDateTime createdAt;
    private String createdBy;
    private List<OptionResponse> options;

    public static PollResponse from(Poll poll) {
        return PollResponse.builder()
                .id(poll.getId())
                .question(poll.getQuestion())
                .createdAt(poll.getCreatedAt())
                .createdBy(poll.getCreatedBy())
                .options(poll.getOptions().stream()
                        .map(OptionResponse::from)  
                        .collect(Collectors.toList()))
                .build();
    }
}