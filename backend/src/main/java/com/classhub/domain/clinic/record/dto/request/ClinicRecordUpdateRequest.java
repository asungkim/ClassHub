package com.classhub.domain.clinic.record.dto.request;

import jakarta.validation.constraints.Size;

public record ClinicRecordUpdateRequest(
        @Size(max = 200) String title,
        String content,
        String homeworkProgress
) {
}
