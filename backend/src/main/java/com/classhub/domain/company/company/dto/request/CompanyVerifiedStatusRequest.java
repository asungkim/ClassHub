package com.classhub.domain.company.company.dto.request;

import jakarta.validation.constraints.NotNull;

public record CompanyVerifiedStatusRequest(
        @NotNull Boolean verified,
        Boolean enabled
) {
}
