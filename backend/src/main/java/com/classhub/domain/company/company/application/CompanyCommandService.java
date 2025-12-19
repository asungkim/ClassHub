package com.classhub.domain.company.company.application;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.dto.request.CompanyCreateRequest;
import com.classhub.domain.company.company.dto.request.CompanyVerifiedStatusRequest;
import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyCommandService {

    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    public CompanyResponse createCompany(UUID teacherId, CompanyCreateRequest request) {
        Objects.requireNonNull(teacherId, "teacherId must not be null");
        VerifiedStatus initialStatus = request.type() == CompanyType.INDIVIDUAL
                ? VerifiedStatus.VERIFIED
                : VerifiedStatus.UNVERIFIED;

        Company company = Company.create(
                request.name(),
                request.description(),
                request.type(),
                initialStatus,
                teacherId
        );
        Company saved = companyRepository.save(company);

        if (request.type() == CompanyType.INDIVIDUAL) {
            String individualBranchName = (request.branchName() != null && !request.branchName().isBlank())
                    ? request.branchName().trim()
                    : saved.getName();
            branchRepository.save(
                    Branch.create(saved.getId(), individualBranchName, teacherId, VerifiedStatus.VERIFIED)
            );
        } else if (request.branchName() != null && !request.branchName().isBlank()) {
            branchRepository.save(
                    Branch.create(saved.getId(), request.branchName().trim(), teacherId, VerifiedStatus.UNVERIFIED)
            );
        }

        return CompanyResponse.from(saved);
    }

    public CompanyResponse updateCompanyVerifiedStatus(UUID companyId, CompanyVerifiedStatusRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(RsCode.COMPANY_NOT_FOUND::toException);

        if (Boolean.TRUE.equals(request.verified())) {
            company.verify();
        } else {
            company.markUnverified();
        }

        if (Boolean.FALSE.equals(request.enabled())) {
            company.delete();
        } else if (Boolean.TRUE.equals(request.enabled())) {
            company.restore();
        }

        Company saved = companyRepository.save(company);
        return CompanyResponse.from(saved);
    }
}
