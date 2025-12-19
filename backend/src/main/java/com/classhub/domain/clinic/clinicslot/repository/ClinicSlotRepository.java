package com.classhub.domain.clinic.clinicslot.repository;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicSlotRepository extends JpaRepository<ClinicSlot, UUID> {
}
