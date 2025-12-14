package com.classhub.domain.calendar.dto.response;

import com.classhub.domain.member.model.MemberRole;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Placeholder DTO for ClinicRecord 확장을 대비한다.
 */
public record CalendarClinicRecordDto(
        UUID id,
        UUID clinicSlotId,
        LocalDate date,
        String note,
        UUID writerId,
        MemberRole writerRole
) {
}
