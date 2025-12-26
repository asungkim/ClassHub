package com.classhub.domain.notice.model;

import com.classhub.global.entity.BaseEntity;
import com.classhub.global.util.KstTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "notice_read",
        indexes = {
                @Index(name = "idx_notice_read_notice", columnList = "notice_id"),
                @Index(name = "idx_notice_read_assistant", columnList = "assistant_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeRead extends BaseEntity {

    @Column(name = "notice_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID noticeId;

    @Column(name = "assistant_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assistantMemberId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @Builder
    private NoticeRead(UUID noticeId,
                       UUID assistantMemberId,
                       LocalDateTime readAt) {
        this.noticeId = Objects.requireNonNull(noticeId, "noticeId must not be null");
        this.assistantMemberId = Objects.requireNonNull(assistantMemberId, "assistantMemberId must not be null");
        this.readAt = readAt == null ? LocalDateTime.now(KstTime.clock()) : readAt;
    }
}
