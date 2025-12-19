package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherBranchAssignmentRepository extends JpaRepository<TeacherBranchAssignment, UUID> {

    Page<TeacherBranchAssignment> findByTeacherMemberId(UUID teacherMemberId, Pageable pageable);

    Page<TeacherBranchAssignment> findByTeacherMemberIdAndDeletedAtIsNull(UUID teacherMemberId, Pageable pageable);

    Page<TeacherBranchAssignment> findByTeacherMemberIdAndDeletedAtIsNotNull(UUID teacherMemberId, Pageable pageable);

    Optional<TeacherBranchAssignment> findByTeacherMemberIdAndBranchId(UUID teacherMemberId, UUID branchId);

    Optional<TeacherBranchAssignment> findByIdAndTeacherMemberId(UUID assignmentId, UUID teacherMemberId);
}
