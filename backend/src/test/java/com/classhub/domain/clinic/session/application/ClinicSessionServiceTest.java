package com.classhub.domain.clinic.session.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.attendance.repository.ClinicAttendanceCountProjection;
import com.classhub.domain.clinic.permission.application.ClinicPermissionValidator;
import com.classhub.domain.clinic.session.dto.request.ClinicSessionEmergencyCreateRequest;
import com.classhub.domain.clinic.session.dto.response.ClinicSessionResponse;
import com.classhub.domain.clinic.session.model.ClinicSession;
import com.classhub.domain.clinic.session.model.ClinicSessionType;
import com.classhub.domain.clinic.session.repository.ClinicSessionRepository;
import com.classhub.domain.clinic.slot.model.ClinicSlot;
import com.classhub.domain.clinic.slot.repository.ClinicSlotRepository;
import com.classhub.domain.company.branch.model.Branch;
import com.classhub.domain.company.branch.repository.BranchRepository;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClinicSessionServiceTest {

    @Mock
    private ClinicSessionRepository clinicSessionRepository;
    @Mock
    private ClinicSlotRepository clinicSlotRepository;
    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;
    @Mock
    private ClinicPermissionValidator clinicPermissionValidator;
    @Mock
    private BranchRepository branchRepository;

    @InjectMocks
    private ClinicSessionService clinicSessionService;

    @Test
    void createRegularSession_shouldCreateFromSlot() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, UUID.randomUUID());
        LocalDate date = LocalDate.of(2024, 3, 4);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slotId, date))
                .willReturn(Optional.empty());
        given(clinicSessionRepository.save(any(ClinicSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClinicSession session = clinicSessionService.createRegularSession(teacherId, slotId, date);

        assertThat(session.getSlotId()).isEqualTo(slotId);
        assertThat(session.getTeacherMemberId()).isEqualTo(teacherId);
        assertThat(session.getBranchId()).isEqualTo(slot.getBranchId());
        assertThat(session.getSessionType()).isEqualTo(ClinicSessionType.REGULAR);
        assertThat(session.getCreatorMemberId()).isNull();
        assertThat(session.getDate()).isEqualTo(date);
        assertThat(session.getStartTime()).isEqualTo(slot.getStartTime());
        assertThat(session.getEndTime()).isEqualTo(slot.getEndTime());
        assertThat(session.getCapacity()).isEqualTo(slot.getDefaultCapacity());
    }

    @Test
    void createRegularSession_shouldThrow_whenSessionAlreadyExists() {
        UUID teacherId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ClinicSlot slot = createSlot(slotId, teacherId, UUID.randomUUID());
        LocalDate date = LocalDate.of(2024, 3, 4);
        ClinicSession existing = createRegularSession(slot, date);

        given(clinicSlotRepository.findByIdAndDeletedAtIsNull(slotId)).willReturn(Optional.of(slot));
        given(clinicSessionRepository.findBySlotIdAndDateAndDeletedAtIsNull(slotId, date))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() -> clinicSessionService.createRegularSession(teacherId, slotId, date))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SESSION_ALREADY_EXISTS);
    }

    @Test
    void createEmergencySession_shouldCreateForTeacher() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicSessionEmergencyCreateRequest request = new ClinicSessionEmergencyCreateRequest(
                branchId,
                null,
                LocalDate.of(2024, 3, 5),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                6
        );
        Branch branch = createBranch(branchId, VerifiedStatus.VERIFIED);

        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(clinicSessionRepository.save(any(ClinicSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClinicSession session = clinicSessionService.createEmergencySession(principal, request);

        assertThat(session.getSlotId()).isNull();
        assertThat(session.getTeacherMemberId()).isEqualTo(teacherId);
        assertThat(session.getBranchId()).isEqualTo(branchId);
        assertThat(session.getSessionType()).isEqualTo(ClinicSessionType.EMERGENCY);
        assertThat(session.getCreatorMemberId()).isEqualTo(teacherId);
        assertThat(session.getCapacity()).isEqualTo(6);
    }

    @Test
    void createEmergencySession_shouldThrow_whenTimeOutsideRange() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicSessionEmergencyCreateRequest request = new ClinicSessionEmergencyCreateRequest(
                branchId,
                null,
                LocalDate.of(2024, 3, 5),
                LocalTime.of(5, 0),
                LocalTime.of(7, 0),
                6
        );

        assertThatThrownBy(() -> clinicSessionService.createEmergencySession(principal, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SESSION_TIME_INVALID);
    }

    @Test
    void cancelSession_shouldMarkCanceled() {
        UUID teacherId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicSession session = createRegularSession(createSlot(UUID.randomUUID(), teacherId, UUID.randomUUID()),
                LocalDate.now().plusDays(1));
        ReflectionTestUtils.setField(session, "id", sessionId);

        given(clinicSessionRepository.findByIdAndDeletedAtIsNull(sessionId))
                .willReturn(Optional.of(session));
        given(clinicSessionRepository.save(any(ClinicSession.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        clinicSessionService.cancelSession(principal, sessionId);

        assertThat(session.isCanceled()).isTrue();
        verify(clinicSessionRepository).save(session);
    }

    @Test
    void getSessions_shouldIncludeAttendanceCount() {
        UUID teacherId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        LocalDate startDate = LocalDate.of(2024, 3, 4);
        LocalDate endDate = LocalDate.of(2024, 3, 10);
        ClinicSession session = createSession(sessionId, teacherId, branchId, startDate);
        Branch branch = createBranch(branchId, VerifiedStatus.VERIFIED);

        given(branchRepository.findById(branchId)).willReturn(Optional.of(branch));
        given(clinicSessionRepository.findByTeacherMemberIdAndBranchIdAndDateRange(
                teacherId,
                branchId,
                startDate,
                endDate
        )).willReturn(List.of(session));
        given(clinicAttendanceRepository.findAttendanceCountsByClinicSessionIds(List.of(sessionId)))
                .willReturn(List.of(new TestAttendanceCountProjection(sessionId, 3L)));

        List<ClinicSessionResponse> responses = clinicSessionService.getSessions(
                principal,
                null,
                branchId,
                startDate,
                endDate
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).attendanceCount()).isEqualTo(3);
    }

    private ClinicSlot createSlot(UUID slotId, UUID teacherId, UUID branchId) {
        ClinicSlot slot = ClinicSlot.builder()
                .teacherMemberId(teacherId)
                .creatorMemberId(teacherId)
                .branchId(branchId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .defaultCapacity(10)
                .build();
        ReflectionTestUtils.setField(slot, "id", slotId);
        return slot;
    }

    private ClinicSession createRegularSession(ClinicSlot slot, LocalDate date) {
        return ClinicSession.builder()
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
    }

    private ClinicSession createSession(UUID sessionId, UUID teacherId, UUID branchId, LocalDate date) {
        ClinicSession session = ClinicSession.builder()
                .slotId(UUID.randomUUID())
                .teacherMemberId(teacherId)
                .branchId(branchId)
                .sessionType(ClinicSessionType.REGULAR)
                .creatorMemberId(null)
                .date(date)
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .capacity(10)
                .canceled(false)
                .build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        return session;
    }

    private static class TestAttendanceCountProjection implements ClinicAttendanceCountProjection {

        private final UUID clinicSessionId;
        private final Long attendanceCount;

        private TestAttendanceCountProjection(UUID clinicSessionId, Long attendanceCount) {
            this.clinicSessionId = clinicSessionId;
            this.attendanceCount = attendanceCount;
        }

        @Override
        public UUID getClinicSessionId() {
            return clinicSessionId;
        }

        @Override
        public Long getAttendanceCount() {
            return attendanceCount;
        }
    }

    private Branch createBranch(UUID branchId, VerifiedStatus status) {
        Branch branch = Branch.create(UUID.randomUUID(), "Branch", UUID.randomUUID(), status);
        ReflectionTestUtils.setField(branch, "id", branchId);
        return branch;
    }
}
