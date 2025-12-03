package com.classhub.domain.invitation.dto;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import java.time.LocalDateTime;

public record InvitationResponse(
        String code,
        String targetEmail,
        InvitationRole inviteeRole,
        InvitationStatus status,
        LocalDateTime expiredAt,
        LocalDateTime createdAt
) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getCode(),
                invitation.getTargetEmail(),
                invitation.getInviteeRole(),
                invitation.getStatus(),
                invitation.getExpiredAt(),
                invitation.getCreatedAt()
        );
    }
}
