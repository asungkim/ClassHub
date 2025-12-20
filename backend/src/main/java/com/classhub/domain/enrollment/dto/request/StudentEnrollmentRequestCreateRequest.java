package com.classhub.domain.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StudentEnrollmentRequestCreateRequest(
                @NotNull UUID courseId,
                @Size(max = 2000) String message) {
}
