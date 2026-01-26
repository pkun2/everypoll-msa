package com.everypoll.pollService.service;

import com.everypoll.pollService.dto.PollCreateRequest;
import com.everypoll.pollService.dto.PollResponse;
import com.everypoll.pollService.dto.PollUpdateRequest;
import com.everypoll.pollService.event.PollEventPublisher;
import com.everypoll.pollService.model.Poll;
import com.everypoll.pollService.model.PollOption;
import com.everypoll.pollService.exception.ResourceNotFoundException;
import com.everypoll.pollService.repository.OptionRepository;
import com.everypoll.pollService.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;
    private final PollEventPublisher eventPublisher;
    private final OptionRepository pollOptionRepository;

    // CREATE
    @Transactional
    public PollResponse createPoll(PollCreateRequest request, String createdBy) {
        Poll poll = Poll.builder()
                .question(request.getQuestion())
                .createdBy(createdBy)
                .build();

        request.getOptionTexts().forEach(optionText -> {
            PollOption option = PollOption.builder()
                    .optionText(optionText)
                    .voteCount(0)
                    .build();
            poll.addOption(option);
        });

        Poll savedPoll = pollRepository.save(poll);

        eventPublisher.publishPollCreated(savedPoll);

        return PollResponse.from(savedPoll);
    }

    // READ (All)
    @Transactional(readOnly = true)
    public List<PollResponse> getAllPolls() {
        return pollRepository.findAll().stream()
                .map(PollResponse::from)
                .collect(Collectors.toList());
    }

    // READ (Single)
    @Transactional(readOnly = true)
    public PollResponse getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .map(PollResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
    }

    // UPDATE
    @Transactional
    public PollResponse updatePoll(Long pollId, PollUpdateRequest request, String username) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        // 권한 검사: 투표를 생성한 사용자만 수정 가능
        if (!poll.getCreatedBy().equals(username)) {
            // throw new AccessDeniedException("이 투표를 수정할 권한이 없습니다.");
             throw new RuntimeException("이 투표를 수정할 권한이 없습니다."); // 임시 예외
        }

        poll.updateQuestion(request.getQuestion());

        poll.getOptions().clear(); // 옵션들 다 지우고 새로 추가
        request.getOptionTexts().forEach(optionText -> {
            PollOption option = PollOption.builder()
                    .optionText(optionText)
                    .build();
            poll.addOption(option);
        });

        return PollResponse.from(poll);
    }

    // DELETE
    @Transactional
    public void deletePoll(Long pollId, String username) {
        log.info("투표 삭제 요청 - pollId: {}, username: {}", pollId, username);

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
        
        // 권한 검사: 투표를 생성한 사용자만 삭제 가능
        if (!poll.getCreatedBy().equals(username)) {
            // throw new AccessDeniedException("이 투표를 삭제할 권한이 없습니다.");
            throw new RuntimeException("이 투표를 삭제할 권한이 없습니다."); // 임시 예외
        }
        
        pollRepository.delete(poll);

        eventPublisher.publishPollDeleted(pollId, username);
    }

    @Transactional
    public void handleUserDeleted(Long userId) {
        log.info("사용자 삭제 처리 - userId: {}", userId);

        // 해당 사용자가 생성한 투표의 createdBy를 익명화
        List<Poll> userPolls = pollRepository.findByCreatedBy(String.valueOf(userId));
        userPolls.forEach(poll -> {
            poll.anonymizeAuthor();
        });
        pollRepository.saveAll(userPolls);

        log.info("사용자 투표 익명화 완료 - userId: {}, pollCount: {}", userId, userPolls.size());
    }

    @Transactional
    public void incrementVoteCount(Long pollId, Long optionId) {
        pollOptionRepository.incrementVoteCount(optionId);
        log.debug("투표 수 증가 - pollId: {}, optionId: {}", pollId, optionId);
    }

    @Transactional
    public void decrementVoteCount(Long pollId, Long optionId) {
        pollOptionRepository.decrementVoteCount(optionId);
        log.debug("투표 수 감소 - pollId: {}, optionId: {}", pollId, optionId);
    }
}