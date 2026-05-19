package com.everypoll.pollService.dto;

import com.everypoll.pollService.model.PollOption;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통해서만 생성하도록 강제
public class OptionResponse {
    private Long id;

    @JsonProperty("text") // 프론트엔드는 "text" 필드를 사용
    private String optionText;

    private Integer displayOrder;

    @Builder.Default
    private Integer voteCount = 0; // 투표수 필드 추가

    public static OptionResponse from(PollOption pollOption) {
        return OptionResponse.builder()
                .id(pollOption.getId())
                .optionText(pollOption.getOptionText())
                .displayOrder(pollOption.getDisplayOrder())
                .voteCount(pollOption.getVoteCount())
                .build();
    }
}