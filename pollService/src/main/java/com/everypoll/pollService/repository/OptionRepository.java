package com.everypoll.pollService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.everypoll.pollService.model.PollOption;

public interface OptionRepository extends JpaRepository<PollOption, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE PollOption p SET p.voteCount = p.voteCount + 1 WHERE p.id = :id")
    int incrementVoteCount(@Param("id") Long id); 

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PollOption p SET p.voteCount = p.voteCount - 1 " +
        "WHERE p.id = :id AND p.voteCount > 0") // 0보다 클 때만 감소 (음수 방지)
    int decrementVoteCount(@Param("id") Long id);
}
