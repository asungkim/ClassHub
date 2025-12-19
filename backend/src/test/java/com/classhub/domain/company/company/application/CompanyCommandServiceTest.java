package com.classhub.domain.company.company.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class CompanyCommandServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private CompanyCommandService companyCommandService;

    @Captor
    private ArgumentCaptor<Branch> branchCaptor;

    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
    }

    @Test
    void createCompany_shouldCreateIndividualWithVerifiedStatusAndAutoBranch() {
        CompanyCreateRequest request = new CompanyCreateRequest("Alice Lab", "desc", CompanyType.INDIVIDUAL, null);
        Company savedCompany = Company.create(request.name(), request.description(), request.type(), VerifiedStatus.VERIFIED, teacherId);
        ReflectionTestUtils.setField(savedCompany, "id", UUID.randomUUID());

        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        CompanyResponse response = companyCommandService.createCompany(teacherId, request);

        assertThat(response.companyId()).isEqualTo(savedCompany.getId());
        assertThat(response.verifiedStatus()).isEqualTo(VerifiedStatus.VERIFIED);

        verify(branchRepository).save(branchCaptor.capture());
        Branch branch = branchCaptor.getValue();
        assertThat(branch.getCompanyId()).isEqualTo(savedCompany.getId());
        assertThat(branch.getVerifiedStatus()).isEqualTo(VerifiedStatus.VERIFIED);
        assertThat(branch.getName()).isEqualTo("Alice Lab");
    }

    @Test
    void createCompany_shouldCreateAcademyWithUnverifiedStatusAndOptionalBranch() {
        CompanyCreateRequest request = new CompanyCreateRequest("러셀", "desc", CompanyType.ACADEMY, "강남");
        Company savedCompany = Company.create(request.name(), request.description(), request.type(), VerifiedStatus.UNVERIFIED, teacherId);
        ReflectionTestUtils.setField(savedCompany, "id", UUID.randomUUID());

        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany);

        CompanyResponse response = companyCommandService.createCompany(teacherId, request);

        assertThat(response.verifiedStatus()).isEqualTo(VerifiedStatus.UNVERIFIED);
        verify(branchRepository).save(branchCaptor.capture());
        Branch branch = branchCaptor.getValue();
        assertThat(branch.getVerifiedStatus()).isEqualTo(VerifiedStatus.UNVERIFIED);
        assertThat(branch.getName()).isEqualTo("강남");
    }

    @Test
    void updateCompanyVerifiedStatus_shouldToggleVerificationAndSoftDelete() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.create("러셀", null, CompanyType.ACADEMY, VerifiedStatus.UNVERIFIED, UUID.randomUUID());
        ReflectionTestUtils.setField(company, "id", companyId);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyVerifiedStatusRequest request = new CompanyVerifiedStatusRequest(true, false);
        CompanyResponse response = companyCommandService.updateCompanyVerifiedStatus(companyId, request);

        assertThat(response.verifiedStatus()).isEqualTo(VerifiedStatus.VERIFIED);
        assertThat(response.deletedAt()).isNotNull();
    }

    @Test
    void updateCompanyVerifiedStatus_shouldThrow_whenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyCommandService.updateCompanyVerifiedStatus(companyId, new CompanyVerifiedStatusRequest(true, true)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COMPANY_NOT_FOUND);
    }
}
