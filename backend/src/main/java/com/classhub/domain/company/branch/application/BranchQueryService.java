package com.classhub.domain.company.branch.application;

import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.global.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchQueryService {

    private final BranchRepository branchRepository;

    public PageResponse<BranchResponse> getBranchesForTeacher(
            UUID teacherId,
            UUID companyId,
            VerifiedStatus status,
            String keyword,
            Pageable pageable
    ) {
        VerifiedStatus resolvedStatus = status != null ? status : VerifiedStatus.VERIFIED;
        UUID creatorFilter = resolvedStatus == VerifiedStatus.UNVERIFIED ? teacherId : null;
        Page<Branch> page = branchRepository.searchBranches(
                companyId,
                resolvedStatus,
                keyword,
                creatorFilter,
                pageable
        );
        return PageResponse.from(page.map(BranchResponse::from));
    }

    public PageResponse<BranchResponse> getBranchesForAdmin(
            UUID companyId,
            VerifiedStatus status,
            String keyword,
            Pageable pageable
    ) {
        Page<Branch> page = branchRepository.searchBranches(
                companyId,
                status,
                keyword,
                null,
                pageable
        );
        return PageResponse.from(page.map(BranchResponse::from));
    }
}
