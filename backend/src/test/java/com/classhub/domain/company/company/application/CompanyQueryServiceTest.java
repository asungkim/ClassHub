package com.classhub.domain.company.company.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanyQueryServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyQueryService companyQueryService;

    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
    }

    @Test
    void getCompaniesForTeacher_shouldDefaultToVerifiedSearch() {
        Company company = Company.create("Verified", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        Page<Company> page = new PageImpl<>(java.util.List.of(company));
        when(companyRepository.searchCompanies(eq(VerifiedStatus.VERIFIED), eq(CompanyType.ACADEMY), eq("러셀"), eq(null), any()))
                .thenReturn(page);

        PageResponse<CompanyResponse> response = companyQueryService.getCompaniesForTeacher(
                teacherId,
                VerifiedStatus.VERIFIED,
                CompanyType.ACADEMY,
                "러셀",
                PageRequest.of(0, 10)
        );

        assertThat(response.content()).hasSize(1);
        verify(companyRepository).searchCompanies(VerifiedStatus.VERIFIED, CompanyType.ACADEMY, "러셀", null, PageRequest.of(0, 10));
    }

    @Test
    void getCompaniesForTeacher_shouldRestrictToCreatorWhenUnverified() {
        Page<Company> page = new PageImpl<>(java.util.List.of());
        when(companyRepository.searchCompanies(eq(VerifiedStatus.UNVERIFIED), eq(null), eq(null), eq(teacherId), any()))
                .thenReturn(page);

        companyQueryService.getCompaniesForTeacher(teacherId, VerifiedStatus.UNVERIFIED, null, null, PageRequest.of(0, 20));

        verify(companyRepository).searchCompanies(VerifiedStatus.UNVERIFIED, null, null, teacherId, PageRequest.of(0, 20));
    }

    @Test
    void getCompaniesForAdmin_shouldQueryWithGivenFilters() {
        when(companyRepository.searchCompanies(eq(null), eq(null), eq("alice"), eq(null), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        companyQueryService.getCompaniesForAdmin(null, null, "alice", PageRequest.of(0, 5));

        verify(companyRepository).searchCompanies(null, null, "alice", null, PageRequest.of(0, 5));
    }

    @Test
    void getCompany_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyQueryService.getCompany(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.COMPANY_NOT_FOUND);
    }

    @Test
    void getCompany_shouldReturnResponse() {
        Company company = Company.create("Alice", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID());
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(company, "id", id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        CompanyResponse response = companyQueryService.getCompany(id);

        assertThat(response.companyId()).isEqualTo(id);
    }
}
