package com.classhub.domain.clinic.clinicsession.repository;

import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicSessionRepository extends JpaRepository<ClinicSession, UUID> {

    Optional<ClinicSession> findBySlotIdAndDateAndDeletedAtIsNull(UUID slotId, LocalDate date);

    @Query("""
            SELECT cs
            FROM ClinicSession cs
            WHERE cs.teacherMemberId = :teacherId
              AND cs.branchId = :branchId
              AND cs.date BETWEEN :startDate AND :endDate
              AND cs.deletedAt IS NULL
            ORDER BY cs.date ASC, cs.startTime ASC, cs.id ASC
            """)
    List<ClinicSession> findByTeacherMemberIdAndBranchIdAndDateRange(
            @Param("teacherId") UUID teacherId,
            @Param("branchId") UUID branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
