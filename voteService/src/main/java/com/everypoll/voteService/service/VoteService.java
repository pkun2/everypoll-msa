package com.everypoll.voteService.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.everypoll.voteService.dto.VoteRequest;
import com.everypoll.voteService.dto.VoteResponse;
import com.everypoll.voteService.event.VoteEventPublisher;
import com.everypoll.voteService.model.Vote;
import com.everypoll.voteService.repository.VoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VoteService {
    private final VoteRepository voteRepository;
    private final VoteAggregationService aggregationService; 
    private final VoteEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String VOTE_LOCK_KEY = "vote:lock:%d:%s"; 
    private static final String USER_VOTED_KEY = "vote:user:%d:%s";
    private static final long LOCK_TIMEOUT_SECONDS = 10;
    
    /**
     * vote: 투표실행
     * 
     * @param VoteRequest VoteRequest DTO
     * @param userId 사용자 id
     * 
     * @return VoteResponse
     */
    @Transactional
    public VoteResponse vote(VoteRequest request, Long userId) {
        Long pollId = request.getPollId();
        Long optionId = request.getOptionId();

        log.info("투표 요청 - pollId: {}, optionId: {}, userId: {}", pollId, optionId, userId);

        // 동시성 제어용 lock
        String lockKey = String.format(VOTE_LOCK_KEY, pollId, userId);
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "locked", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("투표 처리 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            String userVotedKey = String.format(USER_VOTED_KEY, pollId, userId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(userVotedKey))) {
                throw new RuntimeException("해당 게시글에 이미 투표했습니다: " + pollId);
            }

            // DB에서 중복 확인 (캐시 미스 시)
            if (voteRepository.existsByPollIdAndUserId(pollId, userId)) {
                // 캐시에 저장
                redisTemplate.opsForValue().set(userVotedKey, optionId, Duration.ofHours(24));
                throw new RuntimeException("해당 게시글에 이미 투표했습니다: " + pollId);
            }

            // 투표 저장
            Vote vote = Vote.builder()
                    .pollId(pollId)
                    .optionId(optionId)
                    .userId(userId)
                    .build();

            Vote savedVote = voteRepository.save(vote);

            // 캐시에 투표 기록 저장
            redisTemplate.opsForValue().set(userVotedKey, optionId, Duration.ofHours(24));

            // 실시간 집계 업데이트
            aggregationService.incrementVoteCount(pollId, optionId);

            // 이벤트 발행 (Kafka)
            eventPublisher.publishVoteCreated(savedVote.getId(), pollId, optionId, userId);

            log.info("투표 완료 - voteId: {}, pollId: {}, optionId: {}", savedVote.getId(), pollId, optionId);

            return VoteResponse.from(savedVote);

        } finally {
            // 락 해제
            redisTemplate.delete(lockKey);
        }
    }

    /** 
     * cancelVote: 투표 취소
     * 
     * @param pollId 
     * @param userId
     */ 
    @Transactional
    public void cancelVote(Long pollId, Long userId) {
        log.info("투표 취소 요청 - pollId: {}, userId: {}", pollId, userId);

        Vote vote = voteRepository.findByPollIdAndUserId(pollId, userId)
                .orElseThrow(() -> new RuntimeException("투표를 찾을 수 없습니다: " + pollId));

        Long optionId = vote.getOptionId();
        Long voteId = vote.getId();

        // DB에서 삭제
        voteRepository.delete(vote);

        // 캐시에서 삭제
        String userVotedKey = String.format(USER_VOTED_KEY, pollId, userId);
        redisTemplate.delete(userVotedKey);

        // 실시간 집계 업데이트
        aggregationService.decrementVoteCount(pollId, optionId);

        // 이벤트 발행
        eventPublisher.publishVoteCancelled(voteId, pollId, optionId, userId);

        log.info("투표 취소 완료 - pollId: {}, userId: {}", pollId, userId);
    }

    /**
     * changeVote: 투표 변경 (기존 투표 취소 후 새로 투표) 
     * 
     * @param request VoteRequest DTO
     * @param userId 사용자 id
     * 
     * @return vote(request, userId) 투표 취소후 다시 투표하는 방식 
     */
    @Transactional
    public VoteResponse changeVote(VoteRequest request, Long userId) {
        Long pollId = request.getPollId();

        log.info("투표 변경 요청 - pollId: {}, newOptionId: {}, userId: {}", 
                pollId, request.getOptionId(), userId);

        // 기존 투표 확인
        Optional<Vote> existingVote = voteRepository.findByPollIdAndUserId(pollId, userId);

        if (existingVote.isPresent()) {
            // 기존 투표 취소
            cancelVote(pollId, userId);
        }

        // 새로운 투표 실행 (중복 체크 우회를 위해 캐시 삭제)
        String userVotedKey = String.format(USER_VOTED_KEY, pollId, userId);
        redisTemplate.delete(userVotedKey);

        return vote(request, userId);
    }

    /** 
     * hasVoted: 사용자 투표 여부 확인  
     * 
     * @param pollId 투표 게시글 id
     * @param userId 사용자 id
     * 
     * @return redis에 캐시가 해당하는 값이 있으면 true 반환, 캐시에 없으면 투표 여부(voted) 반환
     */
    @Transactional
    public boolean hasVoted(Long pollId, Long userId) {
        // 캐시 먼저 확인
        String userVotedKey = String.format(USER_VOTED_KEY, pollId, userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userVotedKey))) {
            return true;
        }

        // DB 확인
        boolean voted = voteRepository.existsByPollIdAndUserId(pollId, userId);
        
        if (voted) {
            // 캐시에 저장
            Vote vote = voteRepository.findByPollIdAndUserId(pollId, userId).orElse(null);
            if (vote != null) {
                redisTemplate.opsForValue().set(userVotedKey, vote.getOptionId(), Duration.ofHours(24));
            }
        }

        return voted;
    }

    /**
     * getUserVotedOption: 사용자가 선택한 옵션 조회  
     *
     * @param pollId 투표 게시글 id
     * @param userId 사용자 id
     * 
     * @return db 확인 값 반환
     */
    @Transactional(readOnly = true)
    public Long getUserVotedOption(Long pollId, Long userId) {
        // 캐시 먼저 확인
        String userVotedKey = String.format(USER_VOTED_KEY, pollId, userId);
        Object cachedOption = redisTemplate.opsForValue().get(userVotedKey);
        
        if (cachedOption != null) {
            return Long.valueOf(cachedOption.toString());
        }

        // DB 확인
        return voteRepository.findByPollIdAndUserId(pollId, userId)
                .map(Vote::getOptionId)
                .orElse(null);
    }

    /**
     * getUserVoteHistory: 사용자 투표 이력 조회 
     * 
     * @param userId 사용자 id
     * 
     * @return 사용자 투표 이력 반환
     */
    @Transactional(readOnly = true)
    public List<VoteResponse> getUserVoteHistory(Long userId) {
        return voteRepository.findByUserId(userId).stream()
                .map(VoteResponse::from)
                .toList();
    }

    /**
     * getVote: 투표 상세 조회  
     * 
     * @param voteId
     * 
     * @return VoteResponse DTO 반환 
     */
    @Transactional(readOnly = true)
    public VoteResponse getVote(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("투표를 찾을 수 없습니다: " + voteId));
        return VoteResponse.from(vote);
    }

    /**
     * handleUserDeleted: 사용자 삭제 시 처리 (이벤트 핸들러에서 호출)
     * 
     * @param userId 사용자 id
     */
    @Transactional
    public void handleUserDeleted(Long userId) {
        log.info("사용자 삭제 처리 - userId: {}", userId);

        List<Vote> userVotes = voteRepository.findByUserId(userId);
        userVotes.forEach(vote -> {
            vote.anonymizeAuthor();
        });
        voteRepository.saveAll(userVotes);

        log.info("사용자 투표 익명화 완료 - userId: {}, voteCount: {}", userId, userVotes.size());
    }

    /**
     * handlePollDeleted: 투표 삭제 시 처리 (이벤트 핸들러에서 호출)
     * 
     * @param pollId 투표 게시글 id
     */
    @Transactional
    public void handlePollDeleted(Long pollId) {
        log.info("투표 삭제 처리 - pollId: {}", pollId);

        // 해당 투표의 모든 투표 기록 삭제
        List<Vote> votes = voteRepository.findByPollId(pollId);
        voteRepository.deleteAll(votes);

        // 캐시 무효화
        aggregationService.invalidateCache(pollId);

        log.info("투표 기록 삭제 완료 - pollId: {}, voteCount: {}", pollId, votes.size());
    }
}
