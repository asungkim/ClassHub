package com.classhub.domain.company.branch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.global.config.JpaConfig;
import java.util.List;
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
class BranchRepositoryTest {

    @Autowired
    private BranchRepository branchRepository;

    @Test
    void searchBranches_shouldFilterByCompanyStatusAndKeyword_andExcludeSoftDeleted() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        Branch verifiedBranch = branchRepository.save(
                Branch.create(companyId, "강남", UUID.randomUUID(), VerifiedStatus.VERIFIED)
        );
        Branch otherStatusBranch = branchRepository.save(
                Branch.create(companyId, "분당", UUID.randomUUID(), VerifiedStatus.UNVERIFIED)
        );
        Branch otherCompanyBranch = branchRepository.save(
                Branch.create(otherCompanyId, "강남", UUID.randomUUID(), VerifiedStatus.VERIFIED)
        );
        Branch deletedBranch = branchRepository.save(
                Branch.create(companyId, "강남2", UUID.randomUUID(), VerifiedStatus.VERIFIED)
        );
        deletedBranch.delete();
        branchRepository.save(deletedBranch);

        Page<Branch> page = branchRepository.searchBranches(
                companyId,
                VerifiedStatus.VERIFIED,
                "강남",
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Branch::getId)
                .isEqualTo(verifiedBranch.getId());
    }

    @Test
    void findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull_shouldReturnOnlyActiveBranches() {
        UUID companyId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();

        Branch active = branchRepository.save(
                Branch.create(companyId, "강남", creatorId, VerifiedStatus.VERIFIED)
        );
        Branch deleted = branchRepository.save(
                Branch.create(companyId, "수원", creatorId, VerifiedStatus.VERIFIED)
        );
        deleted.delete();
        branchRepository.save(deleted);
        branchRepository.save(
                Branch.create(companyId, "잠실", UUID.randomUUID(), VerifiedStatus.VERIFIED)
        );

        List<Branch> result = branchRepository.findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull(companyId, creatorId);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Branch::getId)
                .isEqualTo(active.getId());
    }

    @Test
    void searchBranches_shouldReturnAllWhenFiltersNull() {
        branchRepository.save(
                Branch.create(UUID.randomUUID(), "강남", UUID.randomUUID(), VerifiedStatus.VERIFIED)
        );
        branchRepository.save(
                Branch.create(UUID.randomUUID(), "분당", UUID.randomUUID(), VerifiedStatus.UNVERIFIED)
        );

        Page<Branch> page = branchRepository.searchBranches(null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void searchBranches_shouldFilterByCreatorWhenProvided() {
        UUID companyId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        branchRepository.save(Branch.create(companyId, "강남", creatorId, VerifiedStatus.UNVERIFIED));
        branchRepository.save(Branch.create(companyId, "잠실", UUID.randomUUID(), VerifiedStatus.UNVERIFIED));

        Page<Branch> page = branchRepository.searchBranches(
                companyId,
                VerifiedStatus.UNVERIFIED,
                null,
                creatorId,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent())
                .hasSize(1)
                .first()
                .extracting(Branch::getCreatorMemberId)
                .isEqualTo(creatorId);
    }
}
