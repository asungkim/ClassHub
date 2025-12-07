package com.classhub.domain.member.dto;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record MemberSummary(
        UUID memberId,
        String email,
        String name,
        MemberRole role,
        boolean active,
        UUID teacherId,
        LocalDateTime createdAt
) {

    public static MemberSummary from(Member member) {
        return new MemberSummary(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole(),
                member.isActive(),
                member.getTeacherId(),
                member.getCreatedAt()
        );
    }
}
