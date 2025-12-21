package com.classhub.domain.clinic.clinicattendance.repository;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicAttendanceRepository extends JpaRepository<ClinicAttendance, UUID> {

    @Query("""
            SELECT
                cs.id AS clinicSessionId,
                ca.id AS clinicAttendanceId,
                scr.courseId AS courseId,
                cs.slotId AS slotId,
                cs.date AS date,
                slot.startTime AS startTime,
                slot.endTime AS endTime,
                cs.canceled AS canceled,
                cr.id AS recordId,
                cr.title AS recordTitle,
                cr.writerId AS recordWriterId,
                cr.content AS recordContent
            FROM ClinicAttendance ca
            JOIN StudentCourseRecord scr ON scr.id = ca.studentCourseRecordId
            JOIN ClinicSession cs ON cs.id = ca.clinicSessionId
            LEFT JOIN ClinicSlot slot ON slot.id = cs.slotId
            LEFT JOIN ClinicRecord cr ON cr.clinicAttendanceId = ca.id
            WHERE ca.studentCourseRecordId IN :recordIds
              AND cs.date BETWEEN :startDate AND :endDate
            ORDER BY cs.date ASC, ca.id ASC
            """)
    List<ClinicAttendanceEventProjection> findEventsByRecordIdsAndDateRange(
            @Param("recordIds") List<UUID> recordIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
