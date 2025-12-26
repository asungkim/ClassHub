package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCourseAssignmentRepository extends JpaRepository<StudentCourseAssignment, UUID> {

    boolean existsByStudentMemberIdAndCourseId(UUID studentMemberId, UUID courseId);

    Optional<StudentCourseAssignment> findByStudentMemberIdAndCourseId(UUID studentMemberId, UUID courseId);

    List<StudentCourseAssignment> findByStudentMemberId(UUID studentMemberId);

    @Query("""
            SELECT sca
            FROM StudentCourseAssignment sca
            JOIN Course c ON c.id = sca.courseId
            WHERE sca.studentMemberId = :studentId
              AND c.deletedAt IS NULL
            ORDER BY sca.assignedAt DESC
            """)
    Page<StudentCourseAssignment> findByStudentMemberIdWithActiveCourse(@Param("studentId") UUID studentMemberId,
                                                                        Pageable pageable);

    @Query("""
            SELECT sca.studentMemberId
            FROM StudentCourseAssignment sca
            WHERE sca.courseId = :courseId
            """)
    List<UUID> findStudentMemberIdsByCourseId(@Param("courseId") UUID courseId);
}
