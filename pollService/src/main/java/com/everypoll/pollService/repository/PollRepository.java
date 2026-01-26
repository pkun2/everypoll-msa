package com.everypoll.pollService.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.everypoll.pollService.model.Poll;
import com.everypoll.pollService.model.PollOption;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<PollOption> findOptionById(Long optionId);
    List<Poll> findByCreatedBy(String createdBy);
}
