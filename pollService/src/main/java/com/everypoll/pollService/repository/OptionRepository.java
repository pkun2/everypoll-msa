package com.everypoll.pollService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.everypoll.pollService.model.PollOption;

public interface OptionRepository extends JpaRepository<PollOption, Long> {
    
}
