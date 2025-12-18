package com.classhub.domain.company.branch.repository;

import com.classhub.domain.company.branch.model.Branch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByCompanyIdAndName(UUID companyId, String name);

    List<Branch> findByCompanyId(UUID companyId);
}
