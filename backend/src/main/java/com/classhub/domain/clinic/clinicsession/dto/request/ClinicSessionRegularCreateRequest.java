package com.classhub.domain.clinic.clinicsession.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClinicSessionRegularCreateRequest(
        @NotNull LocalDate date
) {
}
