package com.everypoll.voteService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.everypoll.voteService.model.Vote;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    /** 
     * exitstsByPollIdAndUserId: 중복 투표 여부 확인  
     * 
     * @param pollId 투표 게시글 id
     * @param userId 사용자 id
     */
    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    /**
     * findByPollIdAndUserId: 사용자의 특정 투표 조회  
     * 
     * @param pollId 투표 게시글 id
     * @param userId 사용자 id
     */
    Optional<Vote> findByPollIdAndUserId(Long pollId, Long userId);

    /**
     * findByPollId: 투표 게시글별 투표 조회 
     *
     * @param pollId 투표 게시글 id 
     */
    List<Vote> findByPollId(Long pollId);

    /**
     * findByUserId: 사용자별 투표 이력 조회
     * 
     * @param userId 사용자 id
     */
    List<Vote> findByUserId(Long userId);

    /**
     * countByPollIdGroupByOptionId: 옵션별 투표 수 조회 
     * 
     * @param pollId 투표 게시글 id
     */
    @Query("SELECT v.optionId, COUNT(v) FROM Vote v WHERE v.pollId = :pollId GROUP BY v.optionId")
    List<Object[]> countByPollIdGroupByOptionId(@Param("pollId") Long pollId);

    /**
     * countByPollId: 투표 게시글별 총 투표 수 
     * 
     * @param pollId 투표 게시글 id
     */
    Long countByPollId(Long pollId);

    /**
     * countByPollIdAndOptionId: 옵션별 투표 수 
     * 
     * @param pollId 투표 게시글 id
     * @param optionId 투표 옵션 id 
     */
    Long countByPollIdAndOptionId(Long pollId, Long optionId);

    /**
     * findTop10ByPollIdOrderByCreatedAtDesc: 최근 투표 조회 
     * 
     * @param pollId 투표 게시글 id
     */ 
    List<Vote> findTop10ByPollIdOrderByCreatedAtDesc(Long pollId);

    /** 
     * deleteByPollIdAndUserId: 투표 삭제 (투표 취소)
     * 
     * @param pollId 투표 게시글 id
     * @param userId 사용자 id
     */
    void deleteByPollIdAndUserId(Long pollId, Long userId);
}
