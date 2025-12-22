package com.classhub.domain.clinic.clinicattendance.repository;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicAttendanceRepository extends JpaRepository<ClinicAttendance, UUID> {

    boolean existsByClinicSessionIdAndStudentCourseRecordId(UUID clinicSessionId, UUID studentCourseRecordId);

    long countByClinicSessionId(UUID clinicSessionId);

    List<ClinicAttendance> findByClinicSessionId(UUID clinicSessionId);

    Optional<ClinicAttendance> findByClinicSessionIdAndStudentCourseRecordIdIn(UUID clinicSessionId,
                                                                               List<UUID> recordIds);

    @Query("""
            SELECT ca
            FROM ClinicAttendance ca
            JOIN ClinicSession cs ON cs.id = ca.clinicSessionId
            WHERE ca.studentCourseRecordId IN :recordIds
              AND cs.date BETWEEN :startDate AND :endDate
            ORDER BY cs.date ASC, cs.startTime ASC, ca.id ASC
            """)
    List<ClinicAttendance> findByStudentCourseRecordIdInAndDateRange(@Param("recordIds") List<UUID> recordIds,
                                                                     @Param("startDate") LocalDate startDate,
                                                                     @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT
                ca.id AS attendanceId,
                cr.id AS recordId,
                scr.id AS studentCourseRecordId,
                m.id AS studentMemberId,
                m.name AS studentName,
                m.phoneNumber AS phoneNumber,
                si.schoolName AS schoolName,
                si.grade AS grade,
                si.parentPhone AS parentPhoneNumber,
                si.birthDate AS birthDate
            FROM ClinicAttendance ca
            JOIN StudentCourseRecord scr ON scr.id = ca.studentCourseRecordId
            JOIN Member m ON m.id = scr.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            LEFT JOIN ClinicRecord cr ON cr.clinicAttendanceId = ca.id
            WHERE ca.clinicSessionId = :sessionId
            ORDER BY m.name ASC, ca.id ASC
            """)
    List<ClinicAttendanceDetailProjection> findDetailsByClinicSessionId(@Param("sessionId") UUID sessionId);

    @Query("""
            SELECT
                cs.id AS clinicSessionId,
                ca.id AS clinicAttendanceId,
                scr.courseId AS courseId,
                cs.slotId AS slotId,
                cs.date AS date,
                cs.startTime AS startTime,
                cs.endTime AS endTime,
                cs.canceled AS canceled,
                cr.id AS recordId,
                cr.title AS recordTitle,
                cr.writerId AS recordWriterId,
                cr.content AS recordContent,
                cr.homeworkProgress AS recordHomeworkProgress
            FROM ClinicAttendance ca
            JOIN StudentCourseRecord scr ON scr.id = ca.studentCourseRecordId
            JOIN ClinicSession cs ON cs.id = ca.clinicSessionId
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

    @Query("""
            SELECT COUNT(ca)
            FROM ClinicAttendance ca
            JOIN ClinicSession cs ON cs.id = ca.clinicSessionId
            WHERE ca.studentCourseRecordId = :recordId
              AND cs.date = :date
              AND cs.startTime < :endTime
              AND cs.endTime > :startTime
            """)
    long countOverlappingAttendances(@Param("recordId") UUID recordId,
                                     @Param("date") LocalDate date,
                                     @Param("startTime") LocalTime startTime,
                                     @Param("endTime") LocalTime endTime);
}
