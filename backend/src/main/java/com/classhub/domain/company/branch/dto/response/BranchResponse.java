package com.classhub.domain.company.branch.dto.response;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record BranchResponse(
        UUID branchId,
        UUID companyId,
        String companyName,
        String name,
        VerifiedStatus verifiedStatus,
        UUID creatorMemberId,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public static BranchResponse from(Branch branch) {
        return from(branch, null);
    }

    public static BranchResponse from(Branch branch, String companyName) {
        return new BranchResponse(
                branch.getId(),
                branch.getCompanyId(),
                companyName,
                branch.getName(),
                branch.getVerifiedStatus(),
                branch.getCreatorMemberId(),
                branch.getCreatedAt(),
                branch.getDeletedAt()
        );
    }
}
