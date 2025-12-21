package com.classhub.domain.progress.course.repository;

import com.classhub.domain.progress.course.model.CourseProgress;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, UUID> {

    @Query("""
            SELECT cp
            FROM CourseProgress cp
            WHERE cp.courseId = :courseId
              AND (
                :cursorCreatedAt IS NULL
                OR cp.createdAt < :cursorCreatedAt
                OR (cp.createdAt = :cursorCreatedAt AND cp.id < :cursorId)
              )
            ORDER BY cp.createdAt DESC, cp.id DESC
            """)
    List<CourseProgress> findRecentByCourseId(@Param("courseId") UUID courseId,
                                              @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
                                              @Param("cursorId") UUID cursorId,
                                              Pageable pageable);

    @Query("""
            SELECT cp
            FROM CourseProgress cp
            JOIN StudentCourseRecord scr ON scr.courseId = cp.courseId
            WHERE scr.studentMemberId = :studentId
              AND scr.deletedAt IS NULL
              AND cp.date BETWEEN :startDate AND :endDate
            ORDER BY cp.date ASC, cp.id ASC
            """)
    List<CourseProgress> findByStudentAndDateRange(@Param("studentId") UUID studentId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT cp
            FROM CourseProgress cp
            WHERE cp.courseId IN :courseIds
              AND cp.date BETWEEN :startDate AND :endDate
            ORDER BY cp.date ASC, cp.id ASC
            """)
    List<CourseProgress> findByCourseIdsAndDateRange(@Param("courseIds") List<UUID> courseIds,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
}
