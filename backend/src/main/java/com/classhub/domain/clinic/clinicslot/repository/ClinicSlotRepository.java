package com.classhub.domain.clinic.clinicslot.repository;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicSlotRepository extends JpaRepository<ClinicSlot, UUID> {

    List<ClinicSlot> findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(UUID teacherMemberId, UUID branchId);

    List<ClinicSlot> findByDeletedAtIsNull();

    List<ClinicSlot> findByIdInAndDeletedAtIsNull(List<UUID> slotIds);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("""
            SELECT cs
            FROM ClinicSlot cs
            WHERE cs.id = :slotId
              AND cs.deletedAt IS NULL
            """)
    Optional<ClinicSlot> findByIdAndDeletedAtIsNullForUpdate(@Param("slotId") UUID slotId);

    Optional<ClinicSlot> findByIdAndDeletedAtIsNull(UUID slotId);
}
