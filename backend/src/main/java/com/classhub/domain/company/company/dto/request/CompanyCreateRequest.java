package com.classhub.domain.company.company.dto.request;

import com.classhub.domain.company.company.model.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyCreateRequest(
        @NotBlank String name,
        String description,
        @NotNull CompanyType type,
        String branchName
) {
}
