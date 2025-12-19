package com.classhub.domain.company.company.repository;

import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByName(String name);

    @Query("""
            SELECT c
            FROM Company c
            WHERE c.deletedAt IS NULL
              AND (:status IS NULL OR c.verifiedStatus = :status)
              AND (:type IS NULL OR c.type = :type)
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:creatorId IS NULL OR c.creatorMemberId = :creatorId)
            """)
    Page<Company> searchCompanies(
            @Param("status") VerifiedStatus status,
            @Param("type") CompanyType type,
            @Param("keyword") String keyword,
            @Param("creatorId") UUID creatorMemberId,
            Pageable pageable
    );
}
