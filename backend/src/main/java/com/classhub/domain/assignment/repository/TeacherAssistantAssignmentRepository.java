package com.classhub.domain.assignment.repository;

import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAssistantAssignmentRepository
        extends JpaRepository<TeacherAssistantAssignment, UUID> {

    Page<TeacherAssistantAssignment> findByTeacherMemberId(UUID teacherMemberId, Pageable pageable);

    Page<TeacherAssistantAssignment> findByTeacherMemberIdAndDeletedAtIsNull(
            UUID teacherMemberId,
            Pageable pageable
    );

    Page<TeacherAssistantAssignment> findByTeacherMemberIdAndDeletedAtIsNotNull(
            UUID teacherMemberId,
            Pageable pageable
    );

    Optional<TeacherAssistantAssignment> findByIdAndTeacherMemberId(UUID id, UUID teacherMemberId);

    List<TeacherAssistantAssignment> findByTeacherMemberIdAndAssistantMemberIdIn(
            UUID teacherMemberId,
            Collection<UUID> assistantMemberIds
    );

    Optional<TeacherAssistantAssignment> findByTeacherMemberIdAndAssistantMemberId(
            UUID teacherMemberId,
            UUID assistantMemberId
    );
}
