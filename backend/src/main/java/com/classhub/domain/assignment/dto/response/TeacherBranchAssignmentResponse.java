package com.classhub.domain.assignment.dto.response;

import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TeacherBranchAssignmentResponse(
        UUID assignmentId,
        UUID branchId,
        String branchName,
        UUID companyId,
        String companyName,
        CompanyType companyType,
        VerifiedStatus verifiedStatus,
        BranchRole role,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}
