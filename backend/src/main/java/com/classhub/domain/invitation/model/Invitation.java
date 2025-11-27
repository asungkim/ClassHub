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

    @Column(nullable = false, length = 120)
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
}
