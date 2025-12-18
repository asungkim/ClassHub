package com.classhub.domain.invitation.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "invitation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invitation_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_invitation_sender", columnList = "sender_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseEntity {

    @Column(name = "sender_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID senderId;

    @Column(nullable = false, length = 120)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationRole inviteeRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder
    private Invitation(
            UUID senderId,
            String targetEmail,
            InvitationRole inviteeRole,
            InvitationStatus status,
            String code,
            LocalDateTime expiredAt
    ) {
        this.senderId = Objects.requireNonNull(senderId, "senderId must not be null");
        this.targetEmail = normalizeEmail(targetEmail);
        this.inviteeRole = Objects.requireNonNull(inviteeRole, "inviteeRole must not be null");
        this.status = status == null ? InvitationStatus.PENDING : status;
        this.code = normalizeCode(code);
        this.expiredAt = Objects.requireNonNull(expiredAt, "expiredAt must not be null");
    }

    public boolean canUse(LocalDateTime currentTime) {
        Objects.requireNonNull(currentTime, "currentTime must not be null");
        return status == InvitationStatus.PENDING
                && !isDeleted()
                && currentTime.isBefore(expiredAt);
    }

    public void markAccepted() {
        if (status == InvitationStatus.ACCEPTED) {
            return;
        }
        this.status = InvitationStatus.ACCEPTED;
        delete();
    }

    public void markExpired() {
        if (status == InvitationStatus.EXPIRED) {
            return;
        }
        this.status = InvitationStatus.EXPIRED;
        delete();
    }

    public void revoke() {
        if (status == InvitationStatus.REVOKED) {
            return;
        }
        this.status = InvitationStatus.REVOKED;
        delete();
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    private String normalizeEmail(String email) {
        Objects.requireNonNull(email, "targetEmail must not be null");
        return email.trim().toLowerCase();
    }

    private String normalizeCode(String rawCode) {
        Objects.requireNonNull(rawCode, "code must not be null");
        return rawCode.trim();
    }
}
