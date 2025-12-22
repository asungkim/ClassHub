package com.classhub.domain.clinic.record.repository;

import com.classhub.domain.clinic.record.model.ClinicRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRecordRepository extends JpaRepository<ClinicRecord, UUID> {

    Optional<ClinicRecord> findByClinicAttendanceId(UUID clinicAttendanceId);

    boolean existsByClinicAttendanceId(UUID clinicAttendanceId);
}
