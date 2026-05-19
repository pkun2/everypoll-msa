package com.everypoll.voteService.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteResult: 투표 결과 응답
 * 
 * @param pollId 
 * @param totalVotes 
 * @param options 
 * @param lastUpdated 
 */
public class VoteResultResponse { 
    private Long pollId;
    private Long totalVotes;
    private List<OptionResult> options;
    private Long lastUpdated;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionResult {
        private Long optionId;
        private String optionText;
        private Long voteCount;
        private Double percentage;
    }
}
