package com.everypoll.common.event.poll;

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
public class PollDeletedEvent extends BaseEvent {
    private Long pollId;
    private String deletedBy;

    public static PollDeletedEvent of(Long pollId, String deletedBy) {
        PollDeletedEvent event = PollDeletedEvent.builder()
            .pollId(pollId)
            .deletedBy(deletedBy)
            .build();
        event.init("POLL_DELETED", "poll-service");
        
        return event;
    }
    
}
