package com.classhub.domain.clinic.clinicattendance.repository;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicAttendanceRepository extends JpaRepository<ClinicAttendance, UUID> {
}
