package com.classhub.domain.clinic.clinicrecord.repository;

import com.classhub.domain.clinic.clinicrecord.model.ClinicRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRecordRepository extends JpaRepository<ClinicRecord, UUID> {
}
