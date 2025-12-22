package com.classhub.domain.clinic.clinicslot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSlotCreateRequest(
        @NotNull UUID branchId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer defaultCapacity
) {
}
