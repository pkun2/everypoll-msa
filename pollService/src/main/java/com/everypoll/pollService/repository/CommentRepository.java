package com.everypoll.pollService.repository;

import com.everypoll.pollService.model.Comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Slice<Comment> findByPollId(Long pollId, Pageable pageable);

    void deleteByPollId(Long pollId);
}
