package com.classhub.domain.auth.dto.response;

import com.classhub.domain.invitation.dto.response.StudentCandidateResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationVerifyResponse(
        UUID inviterId,
        String inviterName,
        String inviteeRole,
        LocalDateTime expiresAt,
        StudentCandidateResponse studentProfile // 학생 초대인 경우만 포함, 조교 초대는 null
) {
}
