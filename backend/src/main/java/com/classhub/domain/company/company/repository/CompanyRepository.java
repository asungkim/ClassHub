package com.classhub.domain.company.company.repository;

import com.classhub.domain.company.company.model.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByName(String name);
}
