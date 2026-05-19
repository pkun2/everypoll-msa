package com.everypoll.pollService.model;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.everypoll.common.model.BaseTimeEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = "id", callSuper = false)
@SQLRestriction("deleted = false")
@ToString(exclude = { "options" })
public class Poll extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @CreatedBy
    private String createdBy;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(nullable = false)
    private boolean isBlind = false;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options = new ArrayList<>();

    @Builder
    public Poll(String title, String description, LocalDateTime endAt, String createdBy, boolean isBlind,
            List<PollOption> options) {
        this.title = title;
        this.description = description;
        this.endAt = endAt;
        this.createdBy = createdBy;
        this.isBlind = isBlind;
        this.options = new ArrayList<>();
    }

    public void addOption(PollOption option) {
        this.options.add(option);
        option.setPoll(this);
    }

    public void update(String title, String description, LocalDateTime endAt) {
        this.title = title;
        this.description = description;
        this.endAt = endAt;
    }

    public void anonymizeAuthor() {
        this.createdBy = "deleted_user";
    }

    public void blind() {
        this.isBlind = true;
    }

    public void delete() {
        if (this.deleted) {
            throw new IllegalStateException("이미 삭제된 투표입니다.");
        }
        this.deleted = true;
    }
}