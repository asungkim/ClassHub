package com.classhub.domain.feedback.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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

    @Builder
    private Feedback(UUID memberId,
                     String content,
                     FeedbackStatus status) {
        this.memberId = Objects.requireNonNull(memberId, "memberId must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.status = status == null ? FeedbackStatus.SUBMITTED : status;
    }

    public void resolve() {
        this.status = FeedbackStatus.RESOLVED;
    }
}
