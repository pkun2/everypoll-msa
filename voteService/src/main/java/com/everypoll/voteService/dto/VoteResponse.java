package com.everypoll.voteService.dto;

import java.time.LocalDateTime;

import com.everypoll.voteService.model.Vote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
/** 
 * VoteResponse: 투표 응답 클래스 
 * 
 * @param id 
 * @param pollId
 * @param optionId 
 * @param userId 
 * @param createdAt 
 * @param message 
 * 
 * @return VoteResponse
 */
public class VoteResponse { 
    private Long id;
    private Long pollId;
    private Long optionId;
    private Long userId;
    private LocalDateTime createdAt;
    private String message;

    public static VoteResponse from(Vote vote) {
        return VoteResponse.builder()
            .id(vote.getId())
            .pollId(vote.getPollId())
            .optionId(vote.getOptionId())
            .userId(vote.getUserId())
            .createdAt(vote.getCreatedAt())
            .message("투표가 성공적으로 등록되었습니다!")
            .build();
    }
}
