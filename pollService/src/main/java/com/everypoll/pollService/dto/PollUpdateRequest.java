package com.everypoll.pollService.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PollUpdateRequest {
    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    private String title;

    private String description;

    @NotEmpty(message = "옵션은 비어 있을 수 없습니다.")
    @Size(min = 2, message = "최소 2개 이상의 선택지가 있어야 합니다.")
    private List<OptionDto> options = new ArrayList<>();

    @NotBlank(message = "마감일은 비어 있을 수 없습니다.")
    private String endAt;

    @Getter
    @NoArgsConstructor
    public static class OptionDto {
        @NotBlank(message = "옵션 텍스트는 비어 있을 수 없습니다.")
        private String text;
        private Integer displayOrder;
    }
}
