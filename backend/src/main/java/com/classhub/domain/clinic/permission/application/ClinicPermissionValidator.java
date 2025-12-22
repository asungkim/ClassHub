package com.classhub.domain.clinic.permission.application;

import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClinicPermissionValidator {

    private final TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    private final TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    private final StudentCourseRecordRepository studentCourseRecordRepository;

    public void ensureTeacherAssignment(UUID teacherId, UUID branchId) {
        TeacherBranchAssignment assignment = teacherBranchAssignmentRepository
                .findByTeacherMemberIdAndBranchId(teacherId, branchId)
                .orElseThrow(() -> new BusinessException(RsCode.FORBIDDEN));
        if (!assignment.isActive()) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    public void ensureAssistantAssignment(UUID assistantId, UUID teacherId) {
        boolean assigned = teacherAssistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId)
                .isPresent();
        if (!assigned) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    public void ensureStudentAccess(UUID studentId, UUID teacherId, UUID branchId) {
        long count = studentCourseRecordRepository.countActiveByStudentAndTeacherAndBranch(
                studentId,
                teacherId,
                branchId
        );
        if (count == 0L) {
            throw new BusinessException(RsCode.FORBIDDEN);
        }
    }

    public void ensureStaffAccess(MemberPrincipal principal, UUID teacherId) {
        if (principal.role() == MemberRole.TEACHER) {
            if (!principal.id().equals(teacherId)) {
                throw new BusinessException(RsCode.FORBIDDEN);
            }
            return;
        }
        if (principal.role() == MemberRole.ASSISTANT) {
            ensureAssistantAssignment(principal.id(), teacherId);
            return;
        }
        throw new BusinessException(RsCode.FORBIDDEN);
    }
}
