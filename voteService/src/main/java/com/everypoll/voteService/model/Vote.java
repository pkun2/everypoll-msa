package com.everypoll.voteService.model;

import com.everypoll.common.model.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "votes", uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_vote_poll_user",
            columnNames = {"poll_id", "user_id"}
        )
    })
public class Vote extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "poll_id", nullable = false)
    private Long pollId;

    @JoinColumn(name = "option_id", nullable = false)
    private Long optionId;

    @JoinColumn(name = "user_id", nullable = false)
    private Long userId;

    public void anonymizeAuthor() {
        this.userId = -1L;
    }
}
