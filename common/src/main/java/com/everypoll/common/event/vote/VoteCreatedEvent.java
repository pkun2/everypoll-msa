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
public class VoteCreatedEvent extends BaseEvent {
    private Long voteId;
    private Long pollId;
    private Long optionId;
    private Long userId;

    public static VoteCreatedEvent of(Long voteId, Long pollId, Long optionId, Long userId) {
        VoteCreatedEvent event = VoteCreatedEvent.builder()
            .voteId(voteId)
            .pollId(pollId)
            .optionId(optionId)
            .userId(userId)
            .build();
        event.init("VOTE_CREATED", "vote-service");
        
        return event;
    }
}
