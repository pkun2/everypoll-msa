package com.everypoll.common.event.vote;

import com.everypoll.common.event.BaseEvent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VoteCancelledEvent extends BaseEvent {
    private Long voteId;
    private Long pollId;
    private Long optionId;
    private Long userId;

    public static VoteCancelledEvent of(Long voteId, Long pollId, Long optionId, Long userId) {
        VoteCancelledEvent event = VoteCancelledEvent.builder()
            .voteId(voteId)
            .pollId(pollId)
            .optionId(optionId)
            .userId(userId)
            .build();
        event.init("VOTE_CANCELLED", "vote-service");

        return event;
    }
}
