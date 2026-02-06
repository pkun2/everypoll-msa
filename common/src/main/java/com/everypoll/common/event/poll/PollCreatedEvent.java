package com.everypoll.common.event.poll;

import java.util.List;

import com.everypoll.common.event.BaseEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PollCreatedEvent extends BaseEvent {
    private Long pollId;
    private String title;
    private String description;
    private String endAt;
    private String createdBy;
    private List<OptionInfo> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionInfo {
        private Long optionId;
        private String optionText;
        private Integer displayOrder;
    }

    public static PollCreatedEvent of(Long pollId, String title, String description, String endAt, String createdBy,
            List<OptionInfo> options) {
        PollCreatedEvent event = PollCreatedEvent.builder()
                .pollId(pollId)
                .title(title)
                .description(description)
                .endAt(endAt)
                .createdBy(createdBy)
                .options(options)
                .build();
        event.init("POLL_CREATED", "poll-service");
        return event;
    }
}
