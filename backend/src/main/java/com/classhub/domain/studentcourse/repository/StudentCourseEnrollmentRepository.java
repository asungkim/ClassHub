package com.classhub.domain.studentcourse.repository;

import com.classhub.domain.studentcourse.model.StudentCourseEnrollment;
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
}
