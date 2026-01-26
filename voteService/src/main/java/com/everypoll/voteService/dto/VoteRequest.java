package com.everypoll.voteService.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteRequest: 투표 요청 클래스 
 * 
 * @param pollId
 * @param optionId
 */
public class VoteRequest { 
    @NotNull(message = "투표 게시글 id는 필수 입니다.")
    private Long pollId;

    @NotNull(message = "투표항목 id는 필수 입니다.")
    private Long optionId;
}
