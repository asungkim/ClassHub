package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherStudentAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherStudentAssignmentRepository extends JpaRepository<TeacherStudentAssignment, UUID> {

    boolean existsByTeacherMemberIdAndStudentMemberIdAndDeletedAtIsNull(UUID teacherMemberId,
                                                                        UUID studentMemberId);

    List<TeacherStudentAssignment> findByStudentMemberIdAndDeletedAtIsNull(UUID studentMemberId);
}
