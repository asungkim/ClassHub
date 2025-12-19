package com.classhub.domain.company.branch.application;

import com.classhub.domain.company.branch.dto.request.BranchCreateRequest;
import com.classhub.domain.company.branch.dto.request.BranchUpdateRequest;
import com.classhub.domain.company.branch.dto.request.BranchVerifiedStatusRequest;
import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.domain.member.model.MemberRole;
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
public class BranchCommandService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    public BranchResponse createBranch(UUID teacherId, BranchCreateRequest request) {
        Objects.requireNonNull(teacherId, "teacherId must not be null");
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(RsCode.COMPANY_NOT_FOUND::toException);

        if (company.isDeleted()) {
            throw new BusinessException(RsCode.COMPANY_NOT_FOUND);
        }
        if (company.getType() == CompanyType.INDIVIDUAL) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        if (company.getVerifiedStatus() == VerifiedStatus.UNVERIFIED
                && !teacherId.equals(company.getCreatorMemberId())) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        Branch branch = Branch.create(
                company.getId(),
                request.name(),
                teacherId,
                VerifiedStatus.UNVERIFIED
        );
        Branch saved = branchRepository.save(branch);
        return BranchResponse.from(saved);
    }

    public BranchResponse updateBranch(UUID memberId,
                                       MemberRole role,
                                       UUID branchId,
                                       BranchUpdateRequest request) {
        if (request == null || !request.hasChanges()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);

        if (role == MemberRole.TEACHER && !branch.isOwnedBy(memberId)) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }

        if (request.name() != null && !request.name().isBlank()) {
            branch.rename(request.name());
        }
        if (request.enabled() != null) {
            if (Boolean.TRUE.equals(request.enabled())) {
                branch.restore();
            } else {
                branch.delete();
            }
        }

        Branch saved = branchRepository.save(branch);
        return BranchResponse.from(saved);
    }

    public BranchResponse updateBranchVerifiedStatus(UUID branchId, BranchVerifiedStatusRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(RsCode.BRANCH_NOT_FOUND::toException);

        if (Boolean.TRUE.equals(request.verified())) {
            branch.verify();
        } else {
            branch.markUnverified();
        }

        if (Boolean.FALSE.equals(request.enabled())) {
            branch.delete();
        } else if (Boolean.TRUE.equals(request.enabled())) {
            branch.restore();
        }

        Branch saved = branchRepository.save(branch);
        return BranchResponse.from(saved);
    }
}
