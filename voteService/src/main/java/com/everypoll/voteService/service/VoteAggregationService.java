package com.everypoll.voteService.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.everypoll.voteService.dto.VoteResultResponse;
import com.everypoll.voteService.dto.VoteStatsResponse;
import com.everypoll.voteService.model.Vote;
import com.everypoll.voteService.repository.VoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoteAggregationService {
    private final VoteRepository voteRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // private static final String VOTE_COUNT_KEY = "vote:count:%d";        // pollId
    private static final String OPTION_COUNT_KEY = "vote:option:%d:%d";  // pollId:optionId
    private static final String TOTAL_COUNT_KEY = "vote:total:%d";       // pollId
    private static final String LAST_UPDATED_KEY = "vote:updated:%d";    // pollId

    /**
     * incrementVoteCount: 투표 수 증가 (실시간)
     * 
     * @param pollId 투표 게시글 id
     * @param optionId 투표 옵션 id
     */
    public void incrementVoteCount(Long pollId, Long optionId) {
        String optionKey = String.format(OPTION_COUNT_KEY, pollId, optionId);
        String totalKey = String.format(TOTAL_COUNT_KEY, pollId);
        String updatedKey = String.format(LAST_UPDATED_KEY, pollId);

        redisTemplate.opsForValue().increment(optionKey);
        redisTemplate.opsForValue().increment(totalKey);
        redisTemplate.opsForValue().set(updatedKey, System.currentTimeMillis());

        log.debug("투표 수 증가 - pollId: {}, optionId: {}", pollId, optionId);
    }

    /**
     * decrementVoteCount: 투표 수 감소 (투표 취소 시)
     * 
     * @param pollId 투표 게시글 id 
     * @param optionId 투표 옵션 id
     */
    public void decrementVoteCount(Long pollId, Long optionId) {
        String optionKey = String.format(OPTION_COUNT_KEY, pollId, optionId);
        String totalKey = String.format(TOTAL_COUNT_KEY, pollId);
        String updatedKey = String.format(LAST_UPDATED_KEY, pollId);

        redisTemplate.opsForValue().decrement(optionKey);
        redisTemplate.opsForValue().decrement(totalKey);
        redisTemplate.opsForValue().set(updatedKey, System.currentTimeMillis());

        log.debug("투표 수 감소 - pollId: {}, optionId: {}", pollId, optionId);
    }

    /**
     * VoteResultResponse: 실시간 투표 결과 조회 
     *  
     * @param pollId 투표 게시글 id
     * @param optionIds 투표 옵션 id들
     * 
     * @return VoteResultResponse
     */
    public VoteResultResponse getVoteResult(Long pollId, List<Long> optionIds) {
        String totalKey = String.format(TOTAL_COUNT_KEY, pollId);
        String updatedKey = String.format(LAST_UPDATED_KEY, pollId);

        // 총 투표 수 조회
        Object totalObj = redisTemplate.opsForValue().get(totalKey);
        Long totalVotes = totalObj != null ? Long.valueOf(totalObj.toString()) : 0L;

        // 캐시 미스 시 DB에서 조회
        if (totalVotes == 0L) {
            totalVotes = voteRepository.countByPollId(pollId);
            if (totalVotes > 0) {
                redisTemplate.opsForValue().set(totalKey, totalVotes, Duration.ofMinutes(5));
            }
        }

        // 옵션별 투표 수 조회
        List<VoteResultResponse.OptionResult> optionResults = new ArrayList<>();
        
        for (Long optionId : optionIds) {
            String optionKey = String.format(OPTION_COUNT_KEY, pollId, optionId);
            Object countObj = redisTemplate.opsForValue().get(optionKey);
            Long voteCount = countObj != null ? Long.valueOf(countObj.toString()) : 0L;

            // 캐시 미스 시 DB에서 조회
            if (voteCount == 0L) {
                voteCount = voteRepository.countByPollIdAndOptionId(pollId, optionId);
                if (voteCount > 0) {
                    redisTemplate.opsForValue().set(optionKey, voteCount, Duration.ofMinutes(5));
                }
            }

            double percentage = totalVotes > 0 ? (voteCount * 100.0) / totalVotes : 0.0;

            optionResults.add(VoteResultResponse.OptionResult.builder()
                    .optionId(optionId)
                    .voteCount(voteCount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        // 마지막 업데이트 시간
        Object updatedObj = redisTemplate.opsForValue().get(updatedKey);
        Long lastUpdated = updatedObj != null ? Long.valueOf(updatedObj.toString()) : System.currentTimeMillis();

        return VoteResultResponse.builder()
                .pollId(pollId)
                .totalVotes(totalVotes)
                .options(optionResults)
                .lastUpdated(lastUpdated)
                .build();
    }

    /**
     * getVoteStats: 투표 통계 조회
     * 
     * @param pollId 투표게시글 id
     * @return VoteStatsResponse
     */
    public VoteStatsResponse getVoteStats(Long pollId) {
        // DB에서 상세 통계 조회
        Long totalVotes = voteRepository.countByPollId(pollId);
        
        // 옵션별 투표 수
        List<Object[]> optionData = voteRepository.countByPollIdGroupByOptionId(pollId);
        Map<String, Long> votesByOption = optionData.stream()
                .collect(Collectors.toMap(
                        row -> "option_" + row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        // 첫 번째/마지막 투표 시간
        List<Vote> votes = voteRepository.findByPollId(pollId);
        LocalDateTime firstVoteAt = votes.stream()
                .map(Vote::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastVoteAt = votes.stream()
                .map(Vote::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return VoteStatsResponse.builder()
                .pollId(pollId)
                .totalVotes(totalVotes)
                .uniqueVoters(totalVotes) // 중복 방지로 인해 동일
                .firstVoteAt(firstVoteAt)
                .lastVoteAt(lastVoteAt)
                .votesByOption(votesByOption)
                .build();
    }

    /**
     * syncCacheWithDatabase: 캐시 동기화 (주기적 실행)
     * 
     * @param void
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void syncCacheWithDatabase() {
        log.debug("캐시 동기화 시작");
        // 필요시 구현: 캐시와 DB 간 불일치 해소
    }

    /**
     * invalidateCache: 특정 투표의 캐시 초기화
     * 
     * @param pollId 투표게시글 id
     */
    public void invalidateCache(Long pollId) {
        String pattern = String.format("vote:*:%d*", pollId);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("캐시 초기화 완료 - pollId: {}, keys: {}", pollId, keys.size());
        }
    }

    /**
     * rebuildCache: DB에서 캐시 재구축
     * 
     * @param pollId 투표 옵션 게시글
     * @param optionIds 투표옵션 id들
     */
    public void rebuildCache(Long pollId, List<Long> optionIds) {
        log.info("캐시 재구축 시작 - pollId: {}", pollId);

        // 총 투표 수
        Long totalVotes = voteRepository.countByPollId(pollId);
        String totalKey = String.format(TOTAL_COUNT_KEY, pollId);
        redisTemplate.opsForValue().set(totalKey, totalVotes, Duration.ofHours(1));

        // 옵션별 투표 수
        for (Long optionId : optionIds) {
            Long count = voteRepository.countByPollIdAndOptionId(pollId, optionId);
            String optionKey = String.format(OPTION_COUNT_KEY, pollId, optionId);
            redisTemplate.opsForValue().set(optionKey, count, Duration.ofHours(1));
        }

        // 마지막 업데이트 시간
        String updatedKey = String.format(LAST_UPDATED_KEY, pollId);
        redisTemplate.opsForValue().set(updatedKey, System.currentTimeMillis());

        log.info("캐시 재구축 완료 - pollId: {}, totalVotes: {}", pollId, totalVotes);
    } 
}
