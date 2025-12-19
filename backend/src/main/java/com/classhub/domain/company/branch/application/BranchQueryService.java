package com.classhub.domain.company.branch.application;

import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.response.PageResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
    private final CompanyRepository companyRepository;

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
        Map<UUID, String> companyNames = resolveCompanyNames(page.getContent());
        return PageResponse.from(page.map(branch -> BranchResponse.from(branch, companyNames.get(branch.getCompanyId()))));
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
        Map<UUID, String> companyNames = resolveCompanyNames(page.getContent());
        return PageResponse.from(page.map(branch -> BranchResponse.from(branch, companyNames.get(branch.getCompanyId()))));
    }

    private Map<UUID, String> resolveCompanyNames(Collection<Branch> branches) {
        if (branches.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UUID> companyIds = branches.stream()
                .map(Branch::getCompanyId)
                .distinct()
                .toList();
        List<Company> companies = StreamSupport.stream(
                companyRepository.findAllById(companyIds).spliterator(),
                false
        ).toList();
        return companies.stream()
                .collect(Collectors.toMap(Company::getId, Company::getName, (left, right) -> left));
    }
}
