package com.everypoll.common.event.poll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollBlindedEvent {
    private Long pollId;
    private String reason;
}
