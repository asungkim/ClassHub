package com.classhub.domain.company.company.dto.response;

import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyResponse(
        UUID companyId,
        String name,
        String description,
        CompanyType type,
        VerifiedStatus verifiedStatus,
        UUID creatorMemberId,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getType(),
                company.getVerifiedStatus(),
                company.getCreatorMemberId(),
                company.getCreatedAt(),
                company.getDeletedAt()
        );
    }
}
