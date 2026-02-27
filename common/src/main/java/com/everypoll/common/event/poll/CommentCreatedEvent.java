package com.everypoll.common.event.poll;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 댓글이 생성되었을 때 발행되는 이벤트
 * rag-agent 등에서 수신하여 요약/통계에 활용함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class CommentCreatedEvent {
    private String type;         // 이벤트 타입 (예: "commentCreated")
    private Long commentId;      // 생성된 댓글 ID
    private Long pollId;         // 해당 투표 ID
    private Long userId;         // 작성자 ID (추후 필터링 등 필요시 활용 가능)
    private String content;      // 댓글 내용 (AI 분석 대상)
    private java.time.LocalDateTime createdAt;
}
