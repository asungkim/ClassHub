package com.classhub.domain.auth.dto.response;

import com.classhub.domain.member.model.Member;
import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherRegisterResponse(
        UUID memberId,
        String email,
        String authority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TeacherRegisterResponse from(Member member) {
        return new TeacherRegisterResponse(
                member.getId(),
                member.getEmail(),
                member.getRole().name(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
