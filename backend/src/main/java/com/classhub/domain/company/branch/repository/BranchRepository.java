package com.classhub.domain.company.branch.repository;

import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByCompanyIdAndName(UUID companyId, String name);

    List<Branch> findByCompanyId(UUID companyId);

    @Query("""
            SELECT b
            FROM Branch b
            WHERE b.deletedAt IS NULL
              AND (:companyId IS NULL OR b.companyId = :companyId)
              AND (:status IS NULL OR b.verifiedStatus = :status)
              AND (:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Branch> searchBranches(
            @Param("companyId") UUID companyId,
            @Param("status") VerifiedStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Branch> findByCompanyIdAndCreatorMemberIdAndDeletedAtIsNull(UUID companyId, UUID creatorMemberId);
}
