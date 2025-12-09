package com.classhub.domain.invitation.dto.response;

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
        LocalDateTime createdAt,
        String studentProfileId,
        String studentName
) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getCode(),
                invitation.getTargetEmail(),
                invitation.getInviteeRole(),
                invitation.getStatus(),
                invitation.getExpiredAt(),
                invitation.getCreatedAt(),
                invitation.getStudentProfileId() != null ? invitation.getStudentProfileId().toString() : null,
                null  // 학생 이름은 서비스에서 조인해서 채워야 함
        );
    }

    public static InvitationResponse from(Invitation invitation, String studentName) {
        return new InvitationResponse(
                invitation.getCode(),
                invitation.getTargetEmail(),
                invitation.getInviteeRole(),
                invitation.getStatus(),
                invitation.getExpiredAt(),
                invitation.getCreatedAt(),
                invitation.getStudentProfileId() != null ? invitation.getStudentProfileId().toString() : null,
                studentName
        );
    }
}
