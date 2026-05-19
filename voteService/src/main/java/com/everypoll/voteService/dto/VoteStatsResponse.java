package com.everypoll.voteService.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * VoteStatsResponse: 투표 현황 응답
 * 
 * @param pollId 투표 게시글 아이디
 * @param totalVotes 총 투표 수
 * @param uniqueVoters 중복제외 투표자 
 * @param firstVoteAt 처음 투표된 날짜 
 * @param lastVoteAt 마지막 투표 날짜
 * @param VotesByOption 투표된 옵션
 * 
 */
public class VoteStatsResponse {
    private Long pollId;
    private Long totalVotes;
    private Long uniqueVoters;
    private LocalDateTime firstVoteAt;
    private LocalDateTime lastVoteAt;
    private Map<String, Long> votesByOption;
}
