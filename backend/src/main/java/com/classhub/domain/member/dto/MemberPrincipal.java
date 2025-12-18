package com.classhub.domain.member.dto;

import com.classhub.domain.member.model.MemberRole;
import java.util.UUID;

public record MemberPrincipal(
        UUID id,
        MemberRole role
) {
}
