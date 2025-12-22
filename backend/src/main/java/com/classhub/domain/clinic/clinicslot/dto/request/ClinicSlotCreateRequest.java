package com.classhub.domain.clinic.clinicslot.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSlotCreateRequest(
        UUID branchId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer defaultCapacity
) {
}
