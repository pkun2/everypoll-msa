package com.everypoll.pollService.dto;

import com.everypoll.pollService.model.PollOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더를 통해서만 생성하도록 강제
public class OptionResponse {
    private Long id;
    private String optionText;

    public static OptionResponse from(PollOption pollOption) {
        return OptionResponse.builder()
            .id(pollOption.getId())
            .optionText(pollOption.getOptionText())
            .build();
    }
}