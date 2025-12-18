package com.classhub.domain.invitation.dto.response;

import com.classhub.domain.invitation.model.Invitation;
import java.time.LocalDateTime;

public record InvitationResponse(
        String code,
        String targetEmail,
        LocalDateTime expiredAt
) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getCode(),
                invitation.getTargetEmail(),
                invitation.getExpiredAt()
        );
    }
}
