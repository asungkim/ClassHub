package com.classhub.domain.company.company.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.global.config.JpaConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CompanyRepositoryTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void searchCompanies_shouldFilterByStatusTypeAndKeyword_andExcludeSoftDeleted() {
        UUID creator = UUID.randomUUID();

        Company verifiedAcademy = companyRepository.save(
                Company.create("러셀 강남", "desc", CompanyType.ACADEMY, VerifiedStatus.VERIFIED, creator)
        );
        Company verifiedIndividual = companyRepository.save(
                Company.create("Alice Lab", null, CompanyType.INDIVIDUAL, VerifiedStatus.VERIFIED, creator)
        );
        Company unverifiedAcademy = companyRepository.save(
                Company.create("러셀 분당", null, CompanyType.ACADEMY, VerifiedStatus.UNVERIFIED, creator)
        );
        Company deletedVerified = companyRepository.save(
                Company.create("러셀 대구", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, creator)
        );
        deletedVerified.delete();
        companyRepository.save(deletedVerified);

        Page<Company> page = companyRepository.searchCompanies(
                VerifiedStatus.VERIFIED,
                CompanyType.ACADEMY,
                "러셀",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Company::getId)
                .isEqualTo(verifiedAcademy.getId());
    }

    @Test
    void findByCreatorMemberIdAndDeletedAtIsNull_shouldReturnOnlyActiveCompanies() {
        UUID creator = UUID.randomUUID();
        Company active = companyRepository.save(
                Company.create("Active Academy", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, creator)
        );
        Company deleted = companyRepository.save(
                Company.create("Deleted Academy", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, creator)
        );
        deleted.delete();
        companyRepository.save(deleted);
        companyRepository.save(
                Company.create("Other Creator", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID())
        );

        Page<Company> page = companyRepository.searchCompanies(
                null,
                null,
                null,
                creator,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Company::getId)
                .isEqualTo(active.getId());
    }

    @Test
    void searchCompanies_shouldReturnAllWhenFiltersNull() {
        companyRepository.save(
                Company.create("Company A", null, CompanyType.ACADEMY, VerifiedStatus.VERIFIED, UUID.randomUUID())
        );
        companyRepository.save(
                Company.create("Company B", null, CompanyType.INDIVIDUAL, VerifiedStatus.UNVERIFIED, UUID.randomUUID())
        );

        Page<Company> page = companyRepository.searchCompanies(null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
