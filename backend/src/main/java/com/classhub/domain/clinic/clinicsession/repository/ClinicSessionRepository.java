package com.classhub.domain.clinic.clinicsession.repository;

import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicSessionRepository extends JpaRepository<ClinicSession, UUID> {
}
