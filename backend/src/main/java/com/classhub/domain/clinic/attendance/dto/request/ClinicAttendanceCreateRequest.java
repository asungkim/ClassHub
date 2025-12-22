package com.classhub.domain.clinic.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClinicAttendanceCreateRequest(
        @NotNull UUID studentCourseRecordId
) {
}
