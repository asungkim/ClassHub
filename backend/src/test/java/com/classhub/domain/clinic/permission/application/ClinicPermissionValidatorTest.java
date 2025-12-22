package com.classhub.domain.clinic.permission.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClinicPermissionValidatorTest {

    @Mock
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;

    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @InjectMocks
    private ClinicPermissionValidator clinicPermissionValidator;

    @Test
    void ensureTeacherAssignment_shouldThrow_whenAssignmentMissing() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        given(teacherBranchAssignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branchId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> clinicPermissionValidator.ensureTeacherAssignment(teacherId, branchId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureAssistantAssignment_shouldThrow_whenNotAssigned() {
        UUID teacherId = UUID.randomUUID();
        UUID assistantId = UUID.randomUUID();

        given(teacherAssistantAssignmentRepository
                .findByTeacherMemberIdAndAssistantMemberIdAndDeletedAtIsNull(teacherId, assistantId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> clinicPermissionValidator.ensureAssistantAssignment(assistantId, teacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureStudentAccess_shouldThrow_whenNoActiveRecord() {
        UUID studentId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        given(studentCourseRecordRepository.countActiveByStudentAndTeacherAndBranch(studentId, teacherId, branchId))
                .willReturn(0L);

        assertThatThrownBy(() -> clinicPermissionValidator.ensureStudentAccess(studentId, teacherId, branchId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void ensureStaffAccess_shouldThrow_whenTeacherMismatch() {
        UUID teacherId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);

        assertThatThrownBy(() -> clinicPermissionValidator.ensureStaffAccess(principal, teacherId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }
}
