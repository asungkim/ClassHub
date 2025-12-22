package com.classhub.domain.clinic.clinicdefaultslot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.clinicattendance.support.ClinicAttendancePolicy;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.clinicslot.model.ClinicSlot;
import com.classhub.domain.clinic.clinicslot.repository.ClinicSlotRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
class ClinicDefaultSlotServiceTest {

    @Mock
    private StudentCourseRecordRepository recordRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ClinicSlotRepository clinicSlotRepository;

    @Mock
    private ClinicSessionRepository clinicSessionRepository;

    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;

    @InjectMocks
    private ClinicDefaultSlotService clinicDefaultSlotService;

    @Test
    void updateDefaultSlotForStudent_shouldCreateAttendances_whenAssigningFirstSlot() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(studentId, courseId, null);
        Course course = createCourse(courseId, teacherId, branchId);
        ClinicSlot slot = createSlot(slotId, teacherId, branchId, DayOfWeek.TUESDAY);
        ClinicAttendancePolicy.WeekRange weekRange = ClinicAttendancePolicy.resolveWeek(LocalDate.now());
        ClinicSession pastSession = createSession(slotId, teacherId, branchId, LocalDate.now().minusDays(1));
        ClinicSession futureSession = createSession(slotId, teacherId, branchId, LocalDate.now().plusDays(1));

        given(recordRepository.findByStudentMemberIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
                .willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record));
        given(recordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId)).willReturn(0L);
        given(clinicSessionRepository.findBySlotIdAndDateRange(slotId, weekRange.startDate(), weekRange.endDate()))
                .willReturn(List.of(pastSession, futureSession));
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(
                futureSession.getId(),
                record.getId()
        )).willReturn(false);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        StudentCourseRecord updated = clinicDefaultSlotService.updateDefaultSlotForStudent(studentId, courseId, slotId);

        assertThat(updated.getDefaultClinicSlotId()).isEqualTo(slotId);
        verify(clinicAttendanceRepository, times(1)).save(any(ClinicAttendance.class));
    }

    @Test
    void applyDefaultSlot_shouldThrow_whenDuplicateSlotSelected() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(studentId, courseId, null);
        StudentCourseRecord otherRecord = createRecord(studentId, UUID.randomUUID(), slotId);
        Course course = createCourse(courseId, teacherId, branchId);
        ClinicSlot slot = createSlot(slotId, teacherId, branchId, DayOfWeek.MONDAY);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record, otherRecord));

        assertThatThrownBy(() -> clinicDefaultSlotService.applyDefaultSlot(record, course, slotId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_DUPLICATED);
        verify(recordRepository, never()).countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId);
    }

    @Test
    void applyDefaultSlot_shouldThrow_whenTimeOverlapDetected() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID otherSlotId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(studentId, courseId, null);
        StudentCourseRecord otherRecord = createRecord(studentId, UUID.randomUUID(), otherSlotId);
        Course course = createCourse(courseId, teacherId, branchId);
        ClinicSlot slot = createSlot(slotId, teacherId, branchId, DayOfWeek.TUESDAY);
        ClinicSlot otherSlot = createSlot(otherSlotId, teacherId, branchId, DayOfWeek.TUESDAY);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record, otherRecord));
        given(clinicSlotRepository.findByIdInAndDeletedAtIsNull(List.of(otherSlotId)))
                .willReturn(List.of(otherSlot));

        assertThatThrownBy(() -> clinicDefaultSlotService.applyDefaultSlot(record, course, slotId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_TIME_OVERLAP);
    }

    @Test
    void applyDefaultSlot_shouldThrow_whenCapacityExceeded() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(studentId, courseId, null);
        Course course = createCourse(courseId, teacherId, branchId);
        ClinicSlot slot = createSlot(slotId, teacherId, branchId, DayOfWeek.FRIDAY);
        ReflectionTestUtils.setField(slot, "defaultCapacity", 2);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record));
        given(recordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId)).willReturn(2L);

        assertThatThrownBy(() -> clinicDefaultSlotService.applyDefaultSlot(record, course, slotId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SLOT_CAPACITY_EXCEEDED);
    }

    @Test
    void applyDefaultSlot_shouldSkipAttendanceCreation_whenChangingSlot() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID previousSlotId = UUID.randomUUID();
        StudentCourseRecord record = createRecord(studentId, courseId, previousSlotId);
        Course course = createCourse(courseId, teacherId, branchId);
        ClinicSlot slot = createSlot(slotId, teacherId, branchId, DayOfWeek.MONDAY);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(recordRepository.findByStudentMemberIdAndDeletedAtIsNull(studentId))
                .willReturn(List.of(record));
        given(recordRepository.countByDefaultClinicSlotIdAndDeletedAtIsNull(slotId)).willReturn(0L);

        clinicDefaultSlotService.applyDefaultSlot(record, course, slotId);

        assertThat(record.getDefaultClinicSlotId()).isEqualTo(slotId);
        verify(clinicSessionRepository, never()).findBySlotIdAndDateRange(any(), any(), any());
    }

    private StudentCourseRecord createRecord(UUID studentId, UUID courseId, UUID defaultSlotId) {
        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId, null, defaultSlotId, null);
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        return record;
    }

    private Course createCourse(UUID courseId, UUID teacherId, UUID branchId) {
        Course course = Course.create(
                branchId,
                teacherId,
                "Math",
                null,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                java.util.Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private ClinicSlot createSlot(UUID slotId,
                                  UUID teacherId,
                                  UUID branchId,
                                  DayOfWeek dayOfWeek) {
        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(3)
                .build();
        ReflectionTestUtils.setField(slot, "id", slotId);
        return slot;
    }

    private ClinicSession createSession(UUID slotId, UUID teacherId, UUID branchId, LocalDate date) {
        ClinicSession session = ClinicSession.builder()
                .slotId(slotId)
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .capacity(3)
                .canceled(false)
                .build();
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
