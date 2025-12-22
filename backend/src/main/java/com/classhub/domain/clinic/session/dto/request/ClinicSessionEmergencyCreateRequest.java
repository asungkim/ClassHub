package com.classhub.domain.clinic.session.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ClinicSessionEmergencyCreateRequest(
        @NotNull UUID branchId,
        UUID teacherId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Min(1) Integer capacity
) {
}
