package com.classhub.domain.member.dto.response;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StudentSummaryResponse(
        UUID memberId,
        String name,
        String email,
        String phoneNumber,
        String schoolName,
        String grade,
        LocalDate birthDate,
        Integer age,
        String parentPhone
) {
}
