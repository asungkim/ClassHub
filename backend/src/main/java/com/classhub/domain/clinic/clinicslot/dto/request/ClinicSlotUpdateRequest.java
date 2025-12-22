package com.classhub.domain.clinic.clinicslot.dto.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ClinicSlotUpdateRequest(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer defaultCapacity
) {
}
