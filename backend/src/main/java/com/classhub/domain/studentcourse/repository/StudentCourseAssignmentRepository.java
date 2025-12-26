package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentCourseAssignmentRepository extends JpaRepository<StudentCourseAssignment, UUID> {

    boolean existsByStudentMemberIdAndCourseId(UUID studentMemberId, UUID courseId);

    Optional<StudentCourseAssignment> findByStudentMemberIdAndCourseId(UUID studentMemberId, UUID courseId);

    List<StudentCourseAssignment> findByStudentMemberId(UUID studentMemberId);

    @Query("""
            SELECT sca.studentMemberId
            FROM StudentCourseAssignment sca
            WHERE sca.courseId = :courseId
            """)
    List<UUID> findStudentMemberIdsByCourseId(@Param("courseId") UUID courseId);
}
