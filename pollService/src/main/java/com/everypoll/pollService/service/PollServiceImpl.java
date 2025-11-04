package com.everypoll.pollService.service;

import com.everypoll.pollService.dto.PollCreateRequest;
import com.everypoll.pollService.dto.PollResponse;
import com.everypoll.pollService.dto.PollUpdateRequest;
import com.everypoll.pollService.model.Poll;
import com.everypoll.pollService.model.PollOption;
import com.everypoll.pollService.exception.ResourceNotFoundException;
import com.everypoll.pollService.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {

    private final PollRepository pollRepository;

    // CREATE
    @Override
    @Transactional
    public PollResponse createPoll(PollCreateRequest request, String createdBy) {
        Poll poll = Poll.builder()
                .question(request.getQuestion())
                .createdBy(createdBy)
                .build();

        request.getOptionTexts().forEach(optionText -> {
            PollOption option = PollOption.builder()
                    .optionText(optionText)
                    .build();
            poll.addOption(option);
        });

        Poll savedPoll = pollRepository.save(poll);
        return PollResponse.from(savedPoll);
    }

    // READ (All)
    @Override
    @Transactional(readOnly = true)
    public List<PollResponse> getAllPolls() {
        return pollRepository.findAll().stream()
                .map(PollResponse::from)
                .collect(Collectors.toList());
    }

    // READ (Single)
    @Override
    @Transactional(readOnly = true)
    public PollResponse getPollById(Long pollId) {
        return pollRepository.findById(pollId)
                .map(PollResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
    }

    // UPDATE
    @Override
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
    @Override
    @Transactional
    public void deletePoll(Long pollId, String username) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
        
        // 권한 검사: 투표를 생성한 사용자만 삭제 가능
        if (!poll.getCreatedBy().equals(username)) {
            // throw new AccessDeniedException("이 투표를 삭제할 권한이 없습니다.");
            throw new RuntimeException("이 투표를 삭제할 권한이 없습니다."); // 임시 예외
        }
        
        pollRepository.delete(poll);
    }
}