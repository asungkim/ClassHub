package com.classhub.domain.invitation.dto.response;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationStatus;
import java.time.LocalDateTime;

public record InvitationSummaryResponse(
        String code,
        String targetEmail,
        InvitationStatus status,
        LocalDateTime expiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static InvitationSummaryResponse from(Invitation invitation) {
        return new InvitationSummaryResponse(
                invitation.getCode(),
                invitation.getTargetEmail(),
                invitation.getStatus(),
                invitation.getExpiredAt(),
                invitation.getCreatedAt(),
                invitation.getUpdatedAt()
        );
    }
}
