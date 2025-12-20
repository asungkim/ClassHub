package com.classhub.domain.enrollment.repository;

import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.enrollment.model.StudentEnrollmentRequest;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentEnrollmentRequestRepository extends JpaRepository<StudentEnrollmentRequest, UUID> {

    boolean existsByStudentMemberIdAndCourseIdAndStatusIn(UUID studentMemberId,
                                                          UUID courseId,
                                                          Collection<EnrollmentStatus> statuses);

    Page<StudentEnrollmentRequest> findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(UUID studentMemberId,
                                                                                        Collection<EnrollmentStatus> statuses,
                                                                                        Pageable pageable);

    @Query("""
            SELECT req
            FROM StudentEnrollmentRequest req
            JOIN Course c ON c.id = req.courseId
            JOIN Member m ON m.id = req.studentMemberId
            WHERE c.teacherMemberId = :teacherId
              AND (:courseId IS NULL OR req.courseId = :courseId)
              AND (:studentName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :studentName, '%')))
              AND req.status IN :statuses
            ORDER BY req.createdAt DESC
            """)
    Page<StudentEnrollmentRequest> searchRequestsForTeacher(@Param("teacherId") UUID teacherId,
                                                            @Param("courseId") UUID courseId,
                                                            @Param("statuses") Collection<EnrollmentStatus> statuses,
                                                            @Param("studentName") String studentName,
                                                            Pageable pageable);

    @Query("""
            SELECT req
            FROM StudentEnrollmentRequest req
            JOIN Course c ON c.id = req.courseId
            JOIN Member m ON m.id = req.studentMemberId
            WHERE c.teacherMemberId IN :teacherIds
              AND (:courseId IS NULL OR req.courseId = :courseId)
              AND (:studentName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :studentName, '%')))
              AND req.status IN :statuses
            ORDER BY req.createdAt DESC
            """)
    Page<StudentEnrollmentRequest> searchRequestsForTeachers(@Param("teacherIds") List<UUID> teacherIds,
                                                             @Param("courseId") UUID courseId,
                                                             @Param("statuses") Collection<EnrollmentStatus> statuses,
                                                             @Param("studentName") String studentName,
                                                             Pageable pageable);
}
