package com.classhub.domain.company.branch.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BranchCreateRequest(
        @NotNull UUID companyId,
        @NotBlank String name
){ }
