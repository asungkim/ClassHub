package com.classhub.domain.company.branch.dto.request;

import jakarta.validation.constraints.NotNull;

public record BranchVerifiedStatusRequest(
        @NotNull Boolean verified,
        Boolean enabled
) {
}
