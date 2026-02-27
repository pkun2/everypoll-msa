package com.everypoll.pollService.model;

import com.everypoll.common.model.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted = false")
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = { "poll" })
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    @Builder
    public Comment(Poll poll, Long userId, String content) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("댓글 내용은 비어있을 수 없습니다.");
        this.poll = poll;
        this.userId = userId;
        this.content = content;
    }

    public void updateContent(String content) {
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("댓글 내용은 비어있을 수 없습니다.");
        this.content = content;
    }

    public void delete() {
        this.deleted = true;
    }
}
