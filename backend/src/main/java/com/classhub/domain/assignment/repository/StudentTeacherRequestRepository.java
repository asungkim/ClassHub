package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.StudentTeacherRequest;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
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

    @Query("""
            SELECT req
            FROM StudentTeacherRequest req
            JOIN Member m ON m.id = req.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE req.teacherMemberId = :teacherId
              AND req.status IN :statuses
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY req.createdAt DESC
            """)
    Page<StudentTeacherRequest> searchRequestsForTeacher(@Param("teacherId") UUID teacherId,
                                                         @Param("statuses") Set<TeacherStudentRequestStatus> statuses,
                                                         @Param("keyword") String keyword,
                                                         Pageable pageable);

    @Query("""
            SELECT req
            FROM StudentTeacherRequest req
            JOIN Member m ON m.id = req.studentMemberId
            JOIN StudentInfo si ON si.memberId = m.id
            WHERE req.teacherMemberId IN :teacherIds
              AND req.status IN :statuses
              AND (:keyword IS NULL
                   OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(si.schoolName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(m.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY req.createdAt DESC
            """)
    Page<StudentTeacherRequest> searchRequestsForTeachers(@Param("teacherIds") List<UUID> teacherIds,
                                                          @Param("statuses") Set<TeacherStudentRequestStatus> statuses,
                                                          @Param("keyword") String keyword,
                                                          Pageable pageable);
}
