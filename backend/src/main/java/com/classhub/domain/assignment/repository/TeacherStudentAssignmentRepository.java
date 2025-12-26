package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherStudentAssignmentRepository extends JpaRepository<TeacherStudentAssignment, UUID> {

    boolean existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(UUID teacherMemberId,
                                                                        UUID studentMemberId);

    List<TeacherStudentAssignment> findByStudentMemberIdAndDeletedAtIsNull(UUID studentMemberId);

    @Query("""
            SELECT tsa
            FROM TeacherStudentAssignment tsa
            JOIN Member m ON m.id = tsa.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE tsa.teacherMemberId = :teacherId
              AND tsa.deletedAt IS NULL
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:excludeIds IS NULL OR tsa.studentMemberId NOT IN :excludeIds)
            ORDER BY tsa.createdAt DESC
            """)
    Page<TeacherStudentAssignment> searchAssignmentsForTeacher(@Param("teacherId") UUID teacherId,
                                                               @Param("keyword") String keyword,
                                                               @Param("excludeIds") List<UUID> excludeIds,
                                                               Pageable pageable);

    @Query("""
            SELECT tsa
            FROM TeacherStudentAssignment tsa
            JOIN Member m ON m.id = tsa.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE tsa.teacherMemberId IN :teacherIds
              AND tsa.deletedAt IS NULL
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY tsa.createdAt DESC
            """)
    Page<TeacherStudentAssignment> searchAssignmentsForTeachers(@Param("teacherIds") List<UUID> teacherIds,
                                                                @Param("keyword") String keyword,
                                                                Pageable pageable);

    @Query("""
            SELECT tsa
            FROM TeacherStudentAssignment tsa
            JOIN Member m ON m.id = tsa.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE tsa.teacherMemberId = :teacherId
              AND tsa.deletedAt IS NULL
              AND (:courseId IS NULL OR EXISTS (
                    SELECT 1
                    FROM StudentCourseAssignment sca
                    WHERE sca.courseId = :courseId
                      AND sca.studentMemberId = tsa.studentMemberId
              ))
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY tsa.createdAt DESC
            """)
    Page<TeacherStudentAssignment> searchAssignmentsForTeacherByCourse(@Param("teacherId") UUID teacherId,
                                                                       @Param("courseId") UUID courseId,
                                                                       @Param("keyword") String keyword,
                                                                       Pageable pageable);

    @Query("""
            SELECT tsa
            FROM TeacherStudentAssignment tsa
            JOIN Member m ON m.id = tsa.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE tsa.teacherMemberId IN :teacherIds
              AND tsa.deletedAt IS NULL
              AND (:courseId IS NULL OR EXISTS (
                    SELECT 1
                    FROM StudentCourseAssignment sca
                    WHERE sca.courseId = :courseId
                      AND sca.studentMemberId = tsa.studentMemberId
              ))
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY tsa.createdAt DESC
            """)
    Page<TeacherStudentAssignment> searchAssignmentsForTeachersByCourse(@Param("teacherIds") List<UUID> teacherIds,
                                                                        @Param("courseId") UUID courseId,
                                                                        @Param("keyword") String keyword,
                                                                        Pageable pageable);

    boolean existsByTeacherMemberIdInAndStudentMemberIdAndDeletedAtIsNull(List<UUID> teacherIds,
                                                                          UUID studentMemberId);
}
