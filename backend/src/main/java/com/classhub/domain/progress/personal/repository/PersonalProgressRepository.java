package com.classhub.domain.progress.personal.repository;

import com.classhub.domain.progress.personal.model.PersonalProgress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalProgressRepository extends JpaRepository<PersonalProgress, UUID> {

    @Query("""
            SELECT pp
            FROM PersonalProgress pp
            WHERE pp.studentCourseRecordId = :recordId
              AND (
                :cursorCreatedAt IS NULL
                OR pp.createdAt < :cursorCreatedAt
                OR (pp.createdAt = :cursorCreatedAt AND pp.id < :cursorId)
              )
            ORDER BY pp.createdAt DESC, pp.id DESC
            """)
    List<PersonalProgress> findRecentByRecordId(@Param("recordId") UUID recordId,
                                                @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                                @Param("cursorId") UUID cursorId,
                                                Pageable pageable);

    @Query("""
            SELECT pp
            FROM PersonalProgress pp
            JOIN StudentCourseRecord scr ON scr.id = pp.studentCourseRecordId
            WHERE scr.studentMemberId = :studentId
              AND scr.deletedAt IS NULL
              AND pp.date BETWEEN :startDate AND :endDate
            ORDER BY pp.date ASC, pp.id ASC
            """)
    List<PersonalProgress> findByStudentAndDateRange(@Param("studentId") UUID studentId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT pp
            FROM PersonalProgress pp
            WHERE pp.studentCourseRecordId IN :recordIds
              AND pp.date BETWEEN :startDate AND :endDate
            ORDER BY pp.date ASC, pp.id ASC
            """)
    List<PersonalProgress> findByRecordIdsAndDateRange(@Param("recordIds") List<UUID> recordIds,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
}
