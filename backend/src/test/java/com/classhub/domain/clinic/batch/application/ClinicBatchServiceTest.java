package com.classhub.domain.clinic.batch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.clinic.attendance.model.ClinicAttendance;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClinicBatchServiceTest {

    @Mock
    private ClinicSlotRepository clinicSlotRepository;

    @Mock
    private ClinicSessionRepository clinicSessionRepository;

    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;

    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;

    @InjectMocks
    private ClinicBatchService clinicBatchService;

    @Test
    void generateWeeklySessions_shouldCreateSessionsForActiveSlots() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot mondaySlot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 5);
        ClinicSlot wednesdaySlot = createSlot(teacherId, branchId, DayOfWeek.WEDNESDAY, 7);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate mondayDate = LocalDate.of(2024, Month.MARCH, 4);
        LocalDate wednesdayDate = LocalDate.of(2024, Month.MARCH, 6);

        given(clinicSlotRepository.findByDeletedAtIsNull())
                .willReturn(List.of(mondaySlot, wednesdaySlot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(mondaySlot.getId(), mondayDate))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(wednesdaySlot.getId(), wednesdayDate))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.save(any(ClinicSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicSession> created = clinicBatchService.generateWeeklySessions(baseDate);

        assertThat(created).hasSize(2);
        ArgumentCaptor<ClinicSession> captor = ArgumentCaptor.forClass(ClinicSession.class);
        verify(clinicSessionRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(ClinicSession::getSessionType)
                .containsOnly(ClinicSessionType.REGULAR);
        assertThat(captor.getAllValues())
                .extracting(ClinicSession::getDate)
                .containsExactlyInAnyOrder(mondayDate, wednesdayDate);
    }

    @Test
    void generateWeeklySessions_shouldSkipWhenSessionAlreadyExists() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 8);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate sessionDate = LocalDate.of(2024, Month.MARCH, 4);
        ClinicSession existing = ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(sessionDate)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();

        given(clinicSlotRepository.findByDeletedAtIsNull())
                .willReturn(List.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate))
                .willReturn(Optional.of(existing));

        List<ClinicSession> created = clinicBatchService.generateWeeklySessions(baseDate);

        assertThat(created).isEmpty();
        verify(clinicSessionRepository, never()).save(any(ClinicSession.class));
    }

    @Test
    void generateWeeklySessions_shouldSkipInvalidSlots() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot invalidCapacitySlot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 0);
        ClinicSlot invalidTimeSlot = createSlot(teacherId, branchId, DayOfWeek.TUESDAY, 3);
        ReflectionTestUtils.setField(invalidTimeSlot, "startTime", LocalTime.of(19, 0));
        ReflectionTestUtils.setField(invalidTimeSlot, "endTime", LocalTime.of(18, 0));

        given(clinicSlotRepository.findByDeletedAtIsNull())
                .willReturn(List.of(invalidCapacitySlot, invalidTimeSlot));

        List<ClinicSession> created = clinicBatchService.generateWeeklySessions(LocalDate.of(2024, Month.MARCH, 6));

        assertThat(created).isEmpty();
        verify(clinicSessionRepository, never()).findBySlotIdAndDateAndDeletedAtIsNull(any(), any());
        verify(clinicSessionRepository, never()).save(any(ClinicSession.class));
    }

    @Test
    void generateWeeklySessions_shouldContinueWhenSessionSaveFails() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot mondaySlot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 5);
        ClinicSlot tuesdaySlot = createSlot(teacherId, branchId, DayOfWeek.TUESDAY, 5);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate mondayDate = LocalDate.of(2024, Month.MARCH, 4);
        LocalDate tuesdayDate = LocalDate.of(2024, Month.MARCH, 5);

        given(clinicSlotRepository.findByDeletedAtIsNull())
                .willReturn(List.of(mondaySlot, tuesdaySlot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(mondaySlot.getId(), mondayDate))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(tuesdaySlot.getId(), tuesdayDate))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.save(any(ClinicSession.class)))
                .willThrow(new DataIntegrityViolationException("dup"))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicSession> created = clinicBatchService.generateWeeklySessions(baseDate);

        assertThat(created).hasSize(1);
        verify(clinicSessionRepository, times(2)).save(any(ClinicSession.class));
    }

    @Test
    void generateWeeklyAttendances_shouldCreateAttendancesForDefaultSlots() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 3);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate sessionDate = LocalDate.of(2024, Month.MARCH, 4);
        ClinicSession session = createSession(slot, sessionDate);
        StudentCourseRecord recordOne = createRecord(slot.getId());
        StudentCourseRecord recordTwo = createRecord(slot.getId());

        given(clinicSlotRepository.findByDeletedAtIsNull()).willReturn(List.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate))
                .willReturn(Optional.of(session));
        given(studentCourseRecordRepository.findByDefaultClinicSlotIdAndDeletedAtIsNull(slot.getId()))
                .willReturn(List.of(recordOne, recordTwo));
        given(clinicAttendanceRepository.countByClinicSessionId(session.getId())).willReturn(0L);
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(session.getId(), recordOne.getId()))
                .willReturn(false);
        given(clinicAttendanceRepository.countOverlappingAttendances(
                recordOne.getId(),
                sessionDate,
                session.getStartTime(),
                session.getEndTime()
        )).willReturn(0L);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicAttendance> created = clinicBatchService.generateWeeklyAttendances(baseDate);

        assertThat(created).hasSize(2);
        verify(clinicAttendanceRepository, times(2)).save(any(ClinicAttendance.class));
    }

    @Test
    void generateWeeklyAttendances_shouldSkipWhenCapacityReached() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 1);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate sessionDate = LocalDate.of(2024, Month.MARCH, 4);
        ClinicSession session = createSession(slot, sessionDate);
        StudentCourseRecord recordOne = createRecord(slot.getId());
        StudentCourseRecord recordTwo = createRecord(slot.getId());

        given(clinicSlotRepository.findByDeletedAtIsNull()).willReturn(List.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate))
                .willReturn(Optional.of(session));
        given(studentCourseRecordRepository.findByDefaultClinicSlotIdAndDeletedAtIsNull(slot.getId()))
                .willReturn(List.of(recordOne, recordTwo));
        given(clinicAttendanceRepository.countByClinicSessionId(session.getId())).willReturn(0L);
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(session.getId(), recordOne.getId()))
                .willReturn(false);
        given(clinicAttendanceRepository.countOverlappingAttendances(
                recordOne.getId(),
                sessionDate,
                session.getStartTime(),
                session.getEndTime()
        )).willReturn(0L);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicAttendance> created = clinicBatchService.generateWeeklyAttendances(baseDate);

        assertThat(created).hasSize(1);
        verify(clinicAttendanceRepository, times(1)).save(any(ClinicAttendance.class));
    }

    @Test
    void generateWeeklyAttendances_shouldContinueWhenAttendanceSaveFails() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.MONDAY, 3);
        LocalDate baseDate = LocalDate.of(2024, Month.MARCH, 6);
        LocalDate sessionDate = LocalDate.of(2024, Month.MARCH, 4);
        ClinicSession session = createSession(slot, sessionDate);
        StudentCourseRecord recordOne = createRecord(slot.getId());
        StudentCourseRecord recordTwo = createRecord(slot.getId());

        given(clinicSlotRepository.findByDeletedAtIsNull()).willReturn(List.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate))
                .willReturn(Optional.of(session));
        given(studentCourseRecordRepository.findByDefaultClinicSlotIdAndDeletedAtIsNull(slot.getId()))
                .willReturn(List.of(recordOne, recordTwo));
        given(clinicAttendanceRepository.countByClinicSessionId(session.getId())).willReturn(0L);
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(session.getId(), recordOne.getId()))
                .willReturn(false);
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(session.getId(), recordTwo.getId()))
                .willReturn(false);
        given(clinicAttendanceRepository.countOverlappingAttendances(
                recordOne.getId(),
                sessionDate,
                session.getStartTime(),
                session.getEndTime()
        )).willReturn(0L);
        given(clinicAttendanceRepository.countOverlappingAttendances(
                recordTwo.getId(),
                sessionDate,
                session.getStartTime(),
                session.getEndTime()
        )).willReturn(0L);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willThrow(new DataIntegrityViolationException("dup"))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicAttendance> created = clinicBatchService.generateWeeklyAttendances(baseDate);

        assertThat(created).hasSize(1);
        verify(clinicAttendanceRepository, times(2)).save(any(ClinicAttendance.class));
    }

    @Test
    void generateRemainingSessionsForSlot_shouldCreateSessionAndAttendances() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.WEDNESDAY, 3);
        LocalDateTime now = LocalDateTime.of(2024, Month.MARCH, 5, 10, 0);
        LocalDate sessionDate = LocalDate.of(2024, Month.MARCH, 6);
        StudentCourseRecord recordOne = createRecord(slot.getId());
        StudentCourseRecord recordTwo = createRecord(slot.getId());

        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slot.getId(), sessionDate))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.save(any(ClinicSession.class))).willAnswer(invocation -> {
            ClinicSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
            return session;
        });
        given(studentCourseRecordRepository.findByDefaultClinicSlotIdAndDeletedAtIsNull(slot.getId()))
                .willReturn(List.of(recordOne, recordTwo));
        given(clinicAttendanceRepository.countByClinicSessionId(any())).willReturn(0L);
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(any(), any()))
                .willReturn(false);
        given(clinicAttendanceRepository.countOverlappingAttendances(any(), any(), any(), any()))
                .willReturn(0L);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        List<ClinicSession> created = clinicBatchService.generateRemainingSessionsForSlot(slot, now);

        assertThat(created).hasSize(1);
        verify(clinicSessionRepository, times(1)).save(any(ClinicSession.class));
        verify(clinicAttendanceRepository, times(2)).save(any(ClinicAttendance.class));
    }

    @Test
    void generateRemainingSessionsForSlot_shouldSkipWhenTimePassed() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        ClinicSlot slot = createSlot(teacherId, branchId, DayOfWeek.TUESDAY, 3);
        LocalDateTime now = LocalDateTime.of(2024, Month.MARCH, 5, 20, 0);

        List<ClinicSession> created = clinicBatchService.generateRemainingSessionsForSlot(slot, now);

        assertThat(created).isEmpty();
        verify(clinicSessionRepository, never()).save(any(ClinicSession.class));
        verify(clinicAttendanceRepository, never()).save(any(ClinicAttendance.class));
    }

    private ClinicSession createSession(ClinicSlot slot, LocalDate date) {
        ClinicSession session = ClinicSession.builder()
                .slotId(slot.getId())
                .teacherMemberId(slot.getTeacherMemberId())
                .branchId(slot.getBranchId())
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getDefaultCapacity())
                .canceled(false)
                .build();
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }

    private StudentCourseRecord createRecord(UUID defaultSlotId) {
        StudentCourseRecord record = StudentCourseRecord.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                defaultSlotId,
                null
        );
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        return record;
    }

    private ClinicSlot createSlot(UUID teacherId,
                                  UUID branchId,
                                  DayOfWeek dayOfWeek,
                                  int capacity) {
        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(capacity)
                .build();
        ReflectionTestUtils.setField(slot, "id", UUID.randomUUID());
        return slot;
    }
}
