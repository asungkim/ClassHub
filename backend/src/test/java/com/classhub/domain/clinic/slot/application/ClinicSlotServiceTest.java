package com.classhub.domain.clinic.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.clinic.batch.application.ClinicBatchService;
import com.classhub.domain.clinic.permission.application.ClinicPermissionValidator;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotCreateRequest;
import com.classhub.domain.clinic.slot.dto.request.ClinicSlotUpdateRequest;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
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
    private ClinicPermissionValidator clinicPermissionValidator;
    @Mock
    private BranchRepository branchRepository;
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ClinicBatchService clinicBatchService;

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
        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(clinicSlotRepository.save(any(ClinicSlot.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(clinicBatchService.generateRemainingSessionsForSlot(any(ClinicSlot.class), any(LocalDateTime.class)))
                .willReturn(List.of());

        ClinicSlot slot = clinicSlotService.createSlot(teacherId, request);

        assertThat(slot.getTeacherMemberId()).isEqualTo(teacherId);
        assertThat(slot.getCreatorMemberId()).isEqualTo(teacherId);
        assertThat(slot.getBranchId()).isEqualTo(branchId);
        assertThat(slot.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(slot.getDefaultCapacity()).isEqualTo(10);
        verify(clinicPermissionValidator).ensureTeacherAssignment(teacherId, branchId);
        verify(clinicBatchService)
                .generateRemainingSessionsForSlot(any(ClinicSlot.class), any(LocalDateTime.class));
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
    void createSlot_shouldThrow_whenOutsideAllowedTimeRange() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlotCreateRequest earlyRequest = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(5, 59),
                LocalTime.of(7, 0),
                10
        );
        ClinicSlotCreateRequest lateRequest = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(21, 0),
                LocalTime.of(22, 1),
                10
        );

        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, earlyRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_TIME_INVALID);
        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, lateRequest))
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
        org.mockito.BDDMockito.willThrow(new BusinessException(RsCode.FORBIDDEN))
                .given(clinicPermissionValidator)
                .ensureTeacherAssignment(teacherId, branchId);

        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.FORBIDDEN);
    }

    @Test
    void createSlot_shouldThrow_whenOverlappingSlotExists() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlotCreateRequest request = new ClinicSlotCreateRequest(
                branchId,
                DayOfWeek.MONDAY,
                LocalTime.of(18, 30),
                LocalTime.of(19, 30),
                10
        );
        Branch branch = createBranch(branchId, VerifiedStatus.VERIFIED);
        ClinicSlot existing = createSlot(UUID.randomUUID(), teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 0));

        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(clinicSlotRepository.findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(teacherId, branchId))
                .willReturn(List.of(existing));

        assertThatThrownBy(() -> clinicSlotService.createSlot(teacherId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CONFLICT);
        verify(clinicSlotRepository, never()).save(any(ClinicSlot.class));
    }

    @Test
    void updateSlot_shouldKeepDefaultSlots_whenScheduleChanges() {
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
        verify(studentCourseRecordRepository, never()).clearDefaultClinicSlotId(slotId);
    }

    @Test
    void updateSlot_shouldThrow_whenOverlappingSlotExists() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 0));
        ClinicSlot otherSlot = createSlot(UUID.randomUUID(), teacherId, DayOfWeek.MONDAY, LocalTime.of(18, 30));
        ClinicSlotUpdateRequest request = new ClinicSlotUpdateRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                10
        );

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(clinicSlotRepository.findByTeacherMemberIdAndBranchIdAndDeletedAtIsNull(
                teacherId,
                slot.getBranchId()
        )).willReturn(List.of(slot, otherSlot));

        assertThatThrownBy(() -> clinicSlotService.updateSlot(teacherId, slotId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CONFLICT);
        verify(clinicSlotRepository, never()).save(any(ClinicSlot.class));
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
        verify(studentCourseRecordRepository).clearDefaultClinicSlotId(slotId);
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
