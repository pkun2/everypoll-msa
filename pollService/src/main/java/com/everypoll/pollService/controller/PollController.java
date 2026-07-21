package com.everypoll.pollService.controller;

import com.everypoll.pollService.service.PollService;

import jakarta.validation.Valid;

import com.everypoll.pollService.dto.PollCreateRequest;
import com.everypoll.pollService.dto.PollResponse;
import com.everypoll.pollService.dto.PollUpdateRequest;

import lombok.RequiredArgsConstructor;

import java.net.URI;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/polls")
@RequiredArgsConstructor 
public class PollController {

    private final PollService pollService;
    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    @GetMapping
    public ResponseEntity<List<PollResponse>> getAllPolls() { // 모든 투표 게시글 가져오기
        logger.info("모든 투표 게시글을 가져옵니다.");
        List<PollResponse> polls = pollService.getAllPolls();
        return ResponseEntity.ok(polls); // 200 ok
    }

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(@Valid @RequestBody PollCreateRequest request, @RequestHeader("X-User-Id") Long userId) { // 게시글 생성
        String currentUsername = String.valueOf(userId);
        logger.info("새로운 투표 게시글을 생성합니다.");

        PollResponse pollResponse = pollService.createPoll(request, currentUsername);
        URI location = URI.create("/api/polls/" + pollResponse.getId());
        logger.info("user:", currentUsername, "location", location);

        return ResponseEntity.created(location).body(pollResponse); // PollResponse 반환
    }

    @PutMapping("/{pollId}")
    public ResponseEntity<PollResponse> updatePoll(@PathVariable Long pollId, @Valid @RequestBody PollUpdateRequest request, @RequestHeader("X-User-Id") Long userId) { // 게시글 수정
        String currentUsername = String.valueOf(userId);

        logger.info("투표 게시글 업데이트 id: {} by user: {}", pollId, currentUsername);
        PollResponse updatedPoll = pollService.updatePoll(pollId, request, currentUsername);

        return ResponseEntity.ok(updatedPoll); // 업데이트 내용 반환
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<PollResponse> getPollById(@PathVariable Long pollId) { // 게시글 id 기반 게시글 조회
        logger.info("투표 게시글 조회 id: {}", pollId);
        PollResponse pollResponse = pollService.getPollById(pollId);
        return ResponseEntity.ok(pollResponse); // PollResponse 반환
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<Void> deletePoll(@PathVariable Long pollId, @RequestHeader("X-User-Id") Long userId) { // 게시글 삭제
        String currentUsername = String.valueOf(userId);

        logger.info("투표 게시글 삭제 id: {} user: {}", pollId, currentUsername);
        pollService.deletePoll(pollId, currentUsername);

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/{pollId}/summary")
    public ResponseEntity<Map<String, String>> getPollSummary(@PathVariable Long pollId) { // AI 댓글 요약 (비동기 별도 조회)
        String summary = pollService.getCommentSummary(pollId);
        return ResponseEntity.ok(Map.of("summary", summary != null ? summary : ""));
    }

    @PatchMapping("/{pollId}/blind")
    public ResponseEntity<Void> blindPoll(@PathVariable Long pollId) { // 게시글 블라인드
        logger.info("투표 서버-내부 블라인드 처리 요청 id: {}", pollId);
        pollService.blindPoll(pollId);
        return ResponseEntity.noContent().build();
    }
}