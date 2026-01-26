package com.everypoll.pollService.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder // 빌더 패턴 추가
@AllArgsConstructor // 빌더를 위한 전체 인자 생성자
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@Table(name = "poll_option")
@ToString(exclude = {"poll"})
public class PollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String optionText;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Builder.Default
    @Column(nullable = false)
    private Integer voteCount = 0;
}