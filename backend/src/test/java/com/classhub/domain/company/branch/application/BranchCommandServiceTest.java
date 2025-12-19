package com.classhub.domain.company.branch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BranchCommandServiceTest {

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private BranchCommandService branchCommandService;

    @Captor
    private ArgumentCaptor<Branch> branchCaptor;

    private UUID teacherId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        companyId = UUID.randomUUID();
    }

    @Test
    void createBranch_shouldCreateUnverifiedBranch_whenAcademyCompanyAccessible() {
        BranchCreateRequest request = new BranchCreateRequest(companyId, "강남");
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchCommandService.createBranch(teacherId, request);

        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.companyName()).isEqualTo("러셀");
        assertThat(response.verifiedStatus()).isEqualTo(VerifiedStatus.UNVERIFIED);
        verify(branchRepository).save(branchCaptor.capture());
        assertThat(branchCaptor.getValue().getCreatorMemberId()).isEqualTo(teacherId);
    }

    @Test
    void createBranch_shouldThrow_whenCompanyNotFound() {
        BranchCreateRequest request = new BranchCreateRequest(companyId, "강남");
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchCommandService.createBranch(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COMPANY_NOT_FOUND);
    }

    @Test
    void createBranch_shouldRejectIndividualCompany() {
        BranchCreateRequest request = new BranchCreateRequest(companyId, "강남");
        Company company = Company.create("개인", null, CompanyType.INDIVIDUAL, VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> branchCommandService.createBranch(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    @Test
    void updateBranch_shouldAllowOwnerToRenameAndToggleEnabled() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", teacherId, VerifiedStatus.UNVERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchUpdateRequest request = new BranchUpdateRequest("잠실", false);
        BranchResponse response = branchCommandService.updateBranch(teacherId, MemberRole.TEACHER, branchId, request);

        assertThat(response.name()).isEqualTo("잠실");
        assertThat(response.companyName()).isEqualTo("러셀");
        assertThat(response.deletedAt()).isNotNull();
    }

    @Test
    void updateBranch_shouldRejectTeacherWithoutOwnership() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", UUID.randomUUID(), VerifiedStatus.UNVERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        BranchUpdateRequest request = new BranchUpdateRequest("잠실", true);

        assertThatThrownBy(() -> branchCommandService.updateBranch(teacherId, MemberRole.TEACHER, branchId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void updateBranchVerifiedStatus_shouldToggleVerificationAndEnabled() {
        UUID branchId = UUID.randomUUID();
        Branch branch = Branch.create(companyId, "강남", UUID.randomUUID(), VerifiedStatus.UNVERIFIED);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(branchRepository.save(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchVerifiedStatusRequest request = new BranchVerifiedStatusRequest(true, true);
        BranchResponse response = branchCommandService.updateBranchVerifiedStatus(branchId, request);

        assertThat(response.verifiedStatus()).isEqualTo(VerifiedStatus.VERIFIED);
        assertThat(response.companyName()).isEqualTo("러셀");
        assertThat(response.deletedAt()).isNull();
    }

    @Test
    void updateBranchVerifiedStatus_shouldThrowWhenNotFound() {
        UUID branchId = UUID.randomUUID();
        when(branchRepository.findById(branchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> branchCommandService.updateBranchVerifiedStatus(branchId, new BranchVerifiedStatusRequest(true, true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BRANCH_NOT_FOUND);
    }
}
