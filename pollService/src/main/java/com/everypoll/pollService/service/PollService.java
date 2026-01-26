package com.everypoll.pollService.service;

import com.everypoll.pollService.dto.PollCreateRequest;
import com.everypoll.pollService.dto.PollResponse;
import com.everypoll.pollService.dto.PollUpdateRequest;

import java.util.List;

public interface PollService {
    PollResponse createPoll(PollCreateRequest request, String username);
    List<PollResponse> getAllPolls();
    PollResponse getPollById(Long pollId);
    PollResponse updatePoll(Long pollId, PollUpdateRequest request, String username);
    void deletePoll(Long pollId, String username);
    void handleUserDeleted(Long userId);
    void incrementVoteCount(Long pollId, Long optionId);
    void decrementVoteCount(Long pollId, Long optionId);
}
