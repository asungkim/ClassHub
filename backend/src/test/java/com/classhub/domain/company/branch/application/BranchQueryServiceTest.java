package com.classhub.domain.company.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.company.branch.dto.response.BranchResponse;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.response.PageResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BranchQueryServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private BranchQueryService branchQueryService;

    @Test
    void getBranchesForTeacher_shouldDefaultVerifiedStatusAndOmitCreatorFilter() {
        UUID teacherId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.VERIFIED);
        Page<Branch> page = new PageImpl<>(List.of(branch), PageRequest.of(0, 10), 1);
        when(branchRepository.searchBranches(eq(companyId), eq(VerifiedStatus.VERIFIED), eq("강남"), eq(null), any(Pageable.class)))
                .thenReturn(page);
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<BranchResponse> response = branchQueryService.getBranchesForTeacher(
                teacherId,
                companyId,
                VerifiedStatus.VERIFIED,
                "강남",
                PageRequest.of(0, 10)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().companyName()).isEqualTo("러셀");
        verify(branchRepository).searchBranches(eq(companyId), eq(VerifiedStatus.VERIFIED), eq("강남"), eq(null), any(Pageable.class));
    }

    @Test
    void getBranchesForTeacher_shouldApplyCreatorFilterForUnverified() {
        UUID teacherId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.UNVERIFIED);
        Page<Branch> page = new PageImpl<>(List.of(branch), PageRequest.of(0, 10), 1);
        when(branchRepository.searchBranches(eq(companyId), eq(VerifiedStatus.UNVERIFIED), eq(null), eq(teacherId), any(Pageable.class)))
                .thenReturn(page);
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<BranchResponse> response = branchQueryService.getBranchesForTeacher(
                teacherId,
                companyId,
                VerifiedStatus.UNVERIFIED,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().companyName()).isEqualTo("러셀");
        verify(branchRepository).searchBranches(eq(companyId), eq(VerifiedStatus.UNVERIFIED), eq(null), eq(teacherId), any(Pageable.class));
    }

    @Test
    void getBranchesForAdmin_shouldNotFilterByCreator() {
        UUID companyId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", UUID.randomUUID(), VerifiedStatus.UNVERIFIED);
        Page<Branch> page = new PageImpl<>(List.of(branch), PageRequest.of(0, 20), 1);
        when(branchRepository.searchBranches(eq(companyId), eq(VerifiedStatus.UNVERIFIED), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page);
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findAllById(any())).thenReturn(List.of(company));

        PageResponse<BranchResponse> response = branchQueryService.getBranchesForAdmin(
                companyId,
                VerifiedStatus.UNVERIFIED,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().companyName()).isEqualTo("러셀");
        verify(branchRepository).searchBranches(eq(companyId), eq(VerifiedStatus.UNVERIFIED), eq(null), eq(null), any(Pageable.class));
    }
}
