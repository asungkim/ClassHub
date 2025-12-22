package com.classhub.domain.clinic.clinicslot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.model.BranchRole;
import com.classhub.domain.assignment.model.TeacherBranchAssignment;
import com.classhub.domain.assignment.repository.TeacherBranchAssignmentRepository;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.clinicslot.dto.request.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.clinicslot.dto.request.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClinicSlotServiceTest {

    @Mock
    private ClinicSlotRepository clinicSlotRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private TeacherBranchAssignmentRepository teacherBranchAssignmentRepository;
    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private ClinicSlotService clinicSlotService;

    @Test
    void createSlot_shouldCreateSlot_whenRequestValid() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                10
        );
        Branch branch = createBranch(branchId, VerifiedStatus.VERIFIED);
        TeacherBranchAssignment assignment = TeacherBranchAssignment.create(teacherId, branchId, BranchRole.OWNER);

        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(teacherBranchAssignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branchId))
                .willReturn(Optional.of(assignment));
        given(clinicSlotRepository.save(any(ClinicSlot.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClinicSlot slot = clinicSlotService.createSlot(teacherId, request);

        assertThat(slot.getTeacherMemberId()).isEqualTo(teacherId);
        assertThat(slot.getCreatorMemberId()).isEqualTo(teacherId);
        assertThat(slot.getBranchId()).isEqualTo(branchId);
        assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(slot.getDefaultCapacity()).isEqualTo(10);
    }

    @Test
    void createSlot_shouldThrow_whenTimeInvalid() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(20, 0),
                LocalTime.of(19, 0),
                10
        );

        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_TIME_INVALID);
    }

    @Test
    void createSlot_shouldThrow_whenTeacherNotAssignedToBranch() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                10
        );
        Branch branch = createBranch(branchId, VerifiedStatus.VERIFIED);

        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(teacherBranchAssignmentRepository.findByTeacherMemberIdAndBranchId(teacherId, branchId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void updateSlot_shouldClearDefaultSlots_whenScheduleChanges() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        ClinicSlotUpdateRequest request = new ClinicSlotUpdateRequest(
                DayOfWeek.TUESDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                10
        );

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(clinicSlotRepository.save(any(ClinicSlot.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClinicSlot updated = clinicSlotService.updateSlot(teacherId, slotId, request);

        assertThat(updated.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        verify(studentCourseRecordRepository).clearDefaultClinicSlotId(slotId);
    }

    @Test
    void updateSlot_shouldThrow_whenCapacityLowerThanAssignments() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        ClinicSlotUpdateRequest request = new ClinicSlotUpdateRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                3
        );

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(studentCourseRecordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId))
                .willReturn(5L);

        assertThatThrownBy(() -> clinicSlotService.updateSlot(teacherId, slotId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CAPACITY_CONFLICT);
        verify(studentCourseRecordRepository, never()).clearDefaultClinicSlotId(slotId);
    }

    @Test
    void deleteSlot_shouldSoftDeleteSlot() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 0));

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(clinicSlotRepository.save(any(ClinicSlot.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        clinicSlotService.deleteSlot(teacherId, slotId);

        assertThat(slot.isDeleted()).isTrue();
    }

    private Branch createBranch(UUID branchId, VerifiedStatus status) {
        Branch branch = Branch.create(UUID.randomUUID(), "Branch", UUID.randomUUID(), status);
        ReflectionTestUtils.setField(branch, "id", branchId);
        return branch;
    }

    private ClinicSlot createSlot(UUID slotId,
                                  UUID teacherId,
                                  DayOfWeek dayOfWeek,
                                  LocalTime startTime) {
        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(UUID.randomUUID())
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .defaultCapacity(10)
                .build();
        ReflectionTestUtils.setField(slot, "id", slotId);
        return slot;
    }
}
