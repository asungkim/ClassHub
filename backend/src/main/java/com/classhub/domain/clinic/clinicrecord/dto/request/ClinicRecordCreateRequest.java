package com.classhub.domain.clinic.clinicrecord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClinicRecordCreateRequest(
        @NotNull UUID clinicAttendanceId,
        @NotBlank String title,
        @NotBlank String content,
        String homeworkProgress
) {
}
