package com.classhub.domain.auth.dto.response;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import java.util.UUID;

public record MeResponse(
        UUID memberId,
        String email,
        String name,
        MemberRole role
) {

    public static MeResponse from(Member member) {
        return new MeResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole()
        );
    }
}
