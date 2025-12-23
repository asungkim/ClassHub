package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCourseEnrollmentRepository extends JpaRepository<StudentCourseEnrollment, UUID> {

    boolean existsByStudentMemberIdAndCourseIdAndDeletedAtIsNull(UUID studentMemberId, UUID courseId);

    @Query("""
            SELECT sce
            FROM StudentCourseEnrollment sce
            JOIN Course c ON c.id = sce.courseId
            WHERE sce.studentMemberId = :studentId
              AND sce.deletedAt IS NULL
              AND c.deletedAt IS NULL
              AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.startDate DESC, c.name ASC
            """)
    Page<StudentCourseEnrollment> searchActiveEnrollments(
            @Param("studentId") UUID studentId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT m.id AS studentMemberId,
                           CASE WHEN EXISTS (
                               SELECT 1
                               FROM StudentCourseEnrollment sceActive
                               JOIN Course cActive ON cActive.id = sceActive.courseId
                               WHERE sceActive.studentMemberId = m.id
                                 AND cActive.teacherMemberId = :teacherId
                                 AND sceActive.deletedAt IS NULL
                                 AND cActive.deletedAt IS NULL
                           ) THEN true ELSE false END AS active
                    FROM StudentCourseEnrollment sce
                    JOIN Course c ON c.id = sce.courseId
                    JOIN Member m ON m.id = sce.studentMemberId
                    WHERE c.teacherMemberId = :teacherId
                      AND sce.deletedAt IS NULL
                      AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:activeOnly = false OR EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId = :teacherId
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                      AND (:inactiveOnly = false OR NOT EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId = :teacherId
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                    GROUP BY m.id, m.name
                    ORDER BY m.name ASC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT m.id)
                    FROM StudentCourseEnrollment sce
                    JOIN Course c ON c.id = sce.courseId
                    JOIN Member m ON m.id = sce.studentMemberId
                    WHERE c.teacherMemberId = :teacherId
                      AND sce.deletedAt IS NULL
                      AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:activeOnly = false OR EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId = :teacherId
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                      AND (:inactiveOnly = false OR NOT EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId = :teacherId
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                    """
    )
    Page<StudentStatusProjection> searchStudentSummariesForTeacher(@Param("teacherId") UUID teacherId,
                                                                   @Param("activeOnly") boolean activeOnly,
                                                                   @Param("inactiveOnly") boolean inactiveOnly,
                                                                   @Param("keyword") String keyword,
                                                                   Pageable pageable);

    @Query(
            value = """
                    SELECT m.id AS studentMemberId,
                           CASE WHEN EXISTS (
                               SELECT 1
                               FROM StudentCourseEnrollment sceActive
                               JOIN Course cActive ON cActive.id = sceActive.courseId
                               WHERE sceActive.studentMemberId = m.id
                                 AND cActive.teacherMemberId IN :teacherIds
                                 AND sceActive.deletedAt IS NULL
                                 AND cActive.deletedAt IS NULL
                           ) THEN true ELSE false END AS active
                    FROM StudentCourseEnrollment sce
                    JOIN Course c ON c.id = sce.courseId
                    JOIN Member m ON m.id = sce.studentMemberId
                    WHERE c.teacherMemberId IN :teacherIds
                      AND sce.deletedAt IS NULL
                      AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:activeOnly = false OR EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId IN :teacherIds
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                      AND (:inactiveOnly = false OR NOT EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId IN :teacherIds
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                    GROUP BY m.id, m.name
                    ORDER BY m.name ASC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT m.id)
                    FROM StudentCourseEnrollment sce
                    JOIN Course c ON c.id = sce.courseId
                    JOIN Member m ON m.id = sce.studentMemberId
                    WHERE c.teacherMemberId IN :teacherIds
                      AND sce.deletedAt IS NULL
                      AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
                      AND (:activeOnly = false OR EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId IN :teacherIds
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                      AND (:inactiveOnly = false OR NOT EXISTS (
                          SELECT 1
                          FROM StudentCourseEnrollment sceActive
                          JOIN Course cActive ON cActive.id = sceActive.courseId
                          WHERE sceActive.studentMemberId = m.id
                            AND cActive.teacherMemberId IN :teacherIds
                            AND sceActive.deletedAt IS NULL
                            AND cActive.deletedAt IS NULL
                      ))
                    """
    )
    Page<StudentStatusProjection> searchStudentSummariesForTeachers(@Param("teacherIds") List<UUID> teacherIds,
                                                                    @Param("activeOnly") boolean activeOnly,
                                                                    @Param("inactiveOnly") boolean inactiveOnly,
                                                                    @Param("keyword") String keyword,
                                                                    Pageable pageable);

    @Query("""
            SELECT sce.studentMemberId AS studentMemberId,
                   c.id AS courseId,
                   c.name AS courseName
            FROM StudentCourseEnrollment sce
            JOIN Course c ON c.id = sce.courseId
            WHERE sce.studentMemberId IN :studentIds
              AND c.teacherMemberId IN :teacherIds
              AND sce.deletedAt IS NULL
              AND c.deletedAt IS NULL
            ORDER BY sce.studentMemberId ASC, c.name ASC
            """)
    List<StudentActiveCourseProjection> findActiveCoursesForStudents(@Param("teacherIds") List<UUID> teacherIds,
                                                                     @Param("studentIds") List<UUID> studentIds);

    @Query("""
            SELECT sce
            FROM StudentCourseEnrollment sce
            JOIN Course c ON c.id = sce.courseId
            WHERE sce.studentMemberId = :studentId
              AND c.teacherMemberId = :teacherId
              AND sce.deletedAt IS NULL
            """)
    List<StudentCourseEnrollment> findByStudentMemberIdAndTeacherMemberId(@Param("studentId") UUID studentId,
                                                                          @Param("teacherId") UUID teacherId);

    @Query("""
            SELECT sce
            FROM StudentCourseEnrollment sce
            JOIN Course c ON c.id = sce.courseId
            WHERE sce.studentMemberId = :studentId
              AND c.teacherMemberId IN :teacherIds
              AND sce.deletedAt IS NULL
            """)
    List<StudentCourseEnrollment> findByStudentMemberIdAndTeacherMemberIdIn(@Param("studentId") UUID studentId,
                                                                            @Param("teacherIds") List<UUID> teacherIds);
}
