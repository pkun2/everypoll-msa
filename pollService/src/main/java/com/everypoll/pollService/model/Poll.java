package com.everypoll.pollService.model;

import jakarta.persistence.*;
import lombok.*;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.everypoll.common.model.BaseTimeEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = "id", callSuper = false)
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
    private java.time.LocalDateTime endAt;

    @CreatedBy
    private String createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> options = new ArrayList<>();

    public void addOption(PollOption option) {
        this.options.add(option);
        option.setPoll(this);
    }

    public void update(String title, String description, java.time.LocalDateTime endAt) {
        this.title = title;
        this.description = description;
        this.endAt = endAt;
    }

    public void anonymizeAuthor() {
        this.createdBy = "deleted_user";
    }
}