package com.classhub.domain.assignment.dto.request;

import com.classhub.domain.assignment.model.BranchRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TeacherBranchAssignmentCreateRequest(
        @NotNull AssignmentCreationMode mode,
        UUID branchId,
        BranchRole role,
        @Valid IndividualInput individual,
        @Valid CompanyInput company,
        @Valid BranchInput branch
) {

    public enum AssignmentCreationMode {
        EXISTING_BRANCH,
        NEW_INDIVIDUAL,
        NEW_COMPANY,
        NEW_BRANCH
    }

    public record IndividualInput(
            @NotBlank String companyName,
            @NotBlank String branchName
    ) {
    }

    public record CompanyInput(
            @NotBlank String companyName,
            @NotBlank String branchName
    ) {
    }

    public record BranchInput(
            @NotNull UUID companyId,
            @NotBlank String branchName
    ) {
    }
}
