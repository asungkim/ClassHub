package com.classhub.domain.enrollment.repository;

import com.classhub.domain.enrollment.model.StudentTeacherRequest;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentTeacherRequestRepository extends JpaRepository<StudentTeacherRequest, UUID> {

    boolean existsByStudentMemberIdAndTeacherMemberIdAndStatusIn(UUID studentMemberId,
                                                                 UUID teacherMemberId,
                                                                 Set<TeacherStudentRequestStatus> statuses);

    Page<StudentTeacherRequest> findByStudentMemberIdAndStatusInOrderByCreatedAtDesc(UUID studentMemberId,
                                                                                      Set<TeacherStudentRequestStatus> statuses,
                                                                                      Pageable pageable);

    @Query("""
            SELECT req.teacherMemberId
            FROM StudentTeacherRequest req
            WHERE req.studentMemberId = :studentId
              AND req.status IN :statuses
            """)
    List<UUID> findTeacherMemberIdsByStudentMemberIdAndStatusIn(
            @Param("studentId") UUID studentMemberId,
            @Param("statuses") Set<TeacherStudentRequestStatus> statuses
    );
}
