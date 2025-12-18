package com.classhub.domain.invitation.dto.response;

import com.classhub.domain.invitation.model.Invitation;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationVerifyResponse(
        UUID senderId,
        String senderName,
        String targetEmail,
        LocalDateTime expiredAt
) {

    public static InvitationVerifyResponse of(
            Invitation invitation,
            UUID senderId,
            String senderName
    ) {
        return new InvitationVerifyResponse(
                senderId,
                senderName,
                invitation.getTargetEmail(),
                invitation.getExpiredAt()
        );
    }
}
