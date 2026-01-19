package com.everypoll.voteService.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "vote_counts")
public class VoteCount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    

    @JoinColumn(name = "poll_id", nullable = false)
    private Long pollId;

    @JoinColumn(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Version
    private Long version;
}
