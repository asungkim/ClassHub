package com.classhub.domain.feedback.model;

import com.classhub.global.entity.BaseEntity;
import com.classhub.global.util.KstTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "feedback",
        indexes = {
                @Index(name = "idx_feedback_member", columnList = "member_id"),
                @Index(name = "idx_feedback_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Column(name = "member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackStatus status;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_member_id", columnDefinition = "BINARY(16)")
    private UUID resolvedByMemberId;

    @Builder
    private Feedback(UUID memberId,
                     String content,
                     FeedbackStatus status,
                     LocalDateTime resolvedAt,
                     UUID resolvedByMemberId) {
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null").trim();
        this.status = status == null ? FeedbackStatus.SUBMITTED : status;
        this.resolvedAt = resolvedAt;
        this.resolvedByMemberId = resolvedByMemberId;
    }

    public void resolve(UUID resolverId, LocalDateTime resolvedAt) {
        this.status = FeedbackStatus.RESOLVED;
        this.resolvedByMemberId = Objects.requireNonNull(resolverId, "resolvedByMemberId must not be null");
        this.resolvedAt = resolvedAt == null ? LocalDateTime.now(KstTime.clock()) : resolvedAt;
    }
}
