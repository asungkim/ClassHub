package com.classhub.domain.invitation.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invitation", indexes = {
        @Index(name = "idx_invitation_sender", columnList = "sender_id"),
        @Index(name = "idx_invitation_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Invitation extends BaseEntity {

    @Column(name = "sender_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID senderId;

    @Column(name = "student_profile_id", columnDefinition = "BINARY(16)")
    private UUID studentProfileId;

    @Column(length = 120)
    private String targetEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationRole inviteeRole;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Builder.Default
    @Column(nullable = false)
    private int useCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private int maxUses = 1;

    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
    }

    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }

    public boolean expireIfPast(LocalDateTime now) {
        if (status == InvitationStatus.PENDING && expiredAt.isBefore(now)) {
            this.status = InvitationStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public boolean canUse(LocalDateTime now) {
        if (status != InvitationStatus.PENDING) {
            return false;
        }
        if (expiredAt.isBefore(now)) {
            return false;
        }
        // maxUses=-1이면 무제한 (조교 초대)
        if (maxUses == -1) {
            return true;
        }
        // useCount < maxUses이면 사용 가능
        return useCount < maxUses;
    }

    public void increaseUseCount() {
        this.useCount++;
    }

    public void acceptIfLimitReached() {
        // maxUses=-1이면 무제한이므로 ACCEPTED로 전환하지 않음
        if (maxUses == -1) {
            return;
        }
        // useCount >= maxUses이면 ACCEPTED
        if (useCount >= maxUses) {
            this.status = InvitationStatus.ACCEPTED;
        }
    }

    public void refresh(
            UUID senderId,
            UUID studentProfileId,
            String targetEmail,
            InvitationRole inviteeRole,
            InvitationStatus status,
            LocalDateTime expiredAt
    ) {
        this.senderId = senderId;
        this.studentProfileId = studentProfileId;
        if (targetEmail != null) {
            this.targetEmail = targetEmail;
        }
        if (inviteeRole != null) {
            this.inviteeRole = inviteeRole;
        }
        if (status != null) {
            this.status = status;
        }
        if (expiredAt != null) {
            this.expiredAt = expiredAt;
        }
    }
}
