package com.everypoll.pollService.controller;

import com.everypoll.pollService.service.PollService;

import jakarta.validation.Valid;

import com.everypoll.pollService.dto.PollCreateRequest;
import com.everypoll.pollService.dto.PollResponse;
import com.everypoll.pollService.dto.PollUpdateRequest;

import lombok.RequiredArgsConstructor;

import java.net.URI;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/polls")
@RequiredArgsConstructor 
public class PollController {

    private final PollService pollService;
    private static final Logger logger = LoggerFactory.getLogger(PollController.class);

    @GetMapping
    public ResponseEntity<List<PollResponse>> getAllPolls() {
        logger.info("모든 투표 게시글을 가져옵니다.");
        List<PollResponse> polls = pollService.getAllPolls();
        return ResponseEntity.ok(polls); // 200 ok
    }

    @PostMapping
    public ResponseEntity<PollResponse> createPoll(@Valid @RequestBody PollCreateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails.getUsername();
        logger.info("새로운 투표 게시글을 생성합니다.");

        PollResponse pollResponse = pollService.createPoll(request, currentUsername);
        URI location = URI.create("/api/polls/" + pollResponse.getId());
        logger.info("user:", currentUsername, "location", location);

        return ResponseEntity.created(location).body(pollResponse); // PollResponse 반환
    }

    @PutMapping("/{pollId}")
    public ResponseEntity<PollResponse> updatePoll(@PathVariable Long pollId, @Valid @RequestBody PollUpdateRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails.getUsername();
        
        logger.info("투표 게시글 업데이트 id: {} by user: {}", pollId, currentUsername);
        PollResponse updatedPoll = pollService.updatePoll(pollId, request, currentUsername);

        return ResponseEntity.ok(updatedPoll); // 업데이트 내용 반환
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<PollResponse> getPollById(@PathVariable Long pollId) {
        logger.info("투표 게시글 조회 id: {}", pollId);
        PollResponse pollResponse = pollService.getPollById(pollId);
        return ResponseEntity.ok(pollResponse); // PollResponse 반환
    }

    @DeleteMapping("/{pollId}")
    public ResponseEntity<Void> deletePoll(@PathVariable Long pollId, @AuthenticationPrincipal UserDetails userDetails) {
        String currentUsername = userDetails.getUsername();
        
        logger.info("투표 게시글 삭제 id: {} user: {}", pollId, currentUsername);
        pollService.deletePoll(pollId, currentUsername);

        return ResponseEntity.noContent().build(); // 204 No Content
    }
}