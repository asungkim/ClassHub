package com.classhub.domain.company.branch.repository;

import com.classhub.domain.company.branch.model.Branch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
}
