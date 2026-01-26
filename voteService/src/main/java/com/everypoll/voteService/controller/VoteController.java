package com.everypoll.voteService.controller;

import com.everypoll.voteService.dto.VoteRequest;
import com.everypoll.voteService.dto.VoteResponse;
import com.everypoll.voteService.dto.VoteResultResponse;
import com.everypoll.voteService.dto.VoteStatsResponse;
import com.everypoll.voteService.service.VoteAggregationService;
import com.everypoll.voteService.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
@Slf4j
public class VoteController {

    private final VoteService voteService;
    private final VoteAggregationService aggregationService;

    /**
     * 투표 실행
     */
    @PostMapping
    public ResponseEntity<VoteResponse> vote(
            @Valid @RequestBody VoteRequest request,
            @RequestHeader(value = "X-User-Id") Long userId,
            HttpServletRequest httpRequest) {

        log.info("POST /api/votes - userId: {}, pollId: {}, optionId: {}",
                userId, request.getPollId(), request.getOptionId());

        VoteResponse response = voteService.vote(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 투표 취소
     */
    @DeleteMapping("/polls/{pollId}")
    public ResponseEntity<Map<String, String>> cancelVote(
            @PathVariable Long pollId,
            @RequestHeader(value = "X-User-Id") Long userId) {

        log.info("DELETE /api/votes/polls/{} - userId: {}", pollId, userId);

        voteService.cancelVote(pollId, userId);
        return ResponseEntity.ok(Map.of("message", "투표가 취소되었습니다"));
    }

    /**
     * 투표 변경
     */
    @PutMapping
    public ResponseEntity<VoteResponse> changeVote(
            @Valid @RequestBody VoteRequest request,
            @RequestHeader(value = "X-User-Id") Long userId,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/votes - userId: {}, pollId: {}, newOptionId: {}",
                userId, request.getPollId(), request.getOptionId());

        VoteResponse response = voteService.changeVote(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 투표 여부 확인
     */
    @GetMapping("/polls/{pollId}/check")
    public ResponseEntity<Map<String, Object>> checkVoted(
            @PathVariable Long pollId,
            @RequestHeader(value = "X-User-Id") Long userId) {

        log.info("GET /api/votes/polls/{}/check - userId: {}", pollId, userId);

        boolean hasVoted = voteService.hasVoted(pollId, userId);
        Long votedOptionId = hasVoted ? voteService.getUserVotedOption(pollId, userId) : null;

        return ResponseEntity.ok(Map.of(
                "pollId", pollId,
                "userId", userId,
                "hasVoted", hasVoted,
                "votedOptionId", votedOptionId != null ? votedOptionId : "null"
        ));
    }

    /**
     * 실시간 투표 결과 조회
     */
    @GetMapping("/polls/{pollId}/results")
    public ResponseEntity<VoteResultResponse> getVoteResult(
            @PathVariable Long pollId,
            @RequestParam List<Long> optionIds) {

        log.info("GET /api/votes/polls/{}/results - optionIds: {}", pollId, optionIds);

        VoteResultResponse response = aggregationService.getVoteResult(pollId, optionIds);
        return ResponseEntity.ok(response);
    }

    /**
     * 투표 통계 조회
     */
    @GetMapping("/polls/{pollId}/stats")
    public ResponseEntity<VoteStatsResponse> getVoteStats(
            @PathVariable Long pollId) {

        log.info("GET /api/votes/polls/{}/stats", pollId);

        VoteStatsResponse response = aggregationService.getVoteStats(pollId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 투표 이력 조회
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<List<VoteResponse>> getUserVoteHistory(
            @PathVariable Long userId) {

        log.info("GET /api/votes/users/{}/history", userId);

        List<VoteResponse> response = voteService.getUserVoteHistory(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 캐시 재구축 (관리자용)
     */
    @PostMapping("/polls/{pollId}/cache/rebuild")
    public ResponseEntity<Map<String, String>> rebuildCache(
            @PathVariable Long pollId,
            @RequestParam List<Long> optionIds) {

        log.info("POST /api/votes/polls/{}/cache/rebuild - optionIds: {}", pollId, optionIds);

        aggregationService.rebuildCache(pollId, optionIds);
        return ResponseEntity.ok(Map.of("message", "캐시 재구축이 완료되었습니다"));
    }
}