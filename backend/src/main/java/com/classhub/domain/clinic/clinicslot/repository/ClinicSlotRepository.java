package com.classhub.domain.clinic.clinicslot.repository;

import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicSlotRepository extends JpaRepository<ClinicSlot, UUID> {

    List<ClinicSlot> findByTeacherId(UUID teacherId);

    List<ClinicSlot> findByTeacherIdAndDayOfWeek(UUID teacherId, DayOfWeek dayOfWeek);

    List<ClinicSlot> findByTeacherIdAndIsActive(UUID teacherId, boolean isActive);

    List<ClinicSlot> findByTeacherIdAndDayOfWeekAndIsActive(UUID teacherId, DayOfWeek dayOfWeek, boolean isActive);

    Optional<ClinicSlot> findByIdAndTeacherId(UUID id, UUID teacherId);

    @Query("""
                SELECT s FROM ClinicSlot s
                WHERE s.teacherId = :teacherId
                AND s.dayOfWeek = :dayOfWeek
                AND s.isActive = true
                AND (:excludeId IS NULL OR s.id <> :excludeId)
                AND (s.startTime < :endTime AND :startTime < s.endTime)
            """)
    List<ClinicSlot> findOverlappingSlots(
            @Param("teacherId") UUID teacherId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);
}
