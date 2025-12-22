package com.classhub.domain.clinic.clinicattendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.clinic.clinicattendance.model.ClinicAttendance;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.clinic.clinicsession.model.ClinicSession;
import com.classhub.domain.clinic.clinicsession.model.ClinicSessionType;
import com.classhub.domain.clinic.clinicsession.repository.ClinicSessionRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.domain.studentcourse.repository.StudentCourseRecordRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClinicAttendanceServiceTest {

    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;
    @Mock
    private ClinicSessionRepository clinicSessionRepository;
    @Mock
    private StudentCourseRecordRepository studentCourseRecordRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherAssistantAssignmentRepository teacherAssistantAssignmentRepository;

    @InjectMocks
    private ClinicAttendanceService clinicAttendanceService;

    @Test
    void addAttendance_shouldCreate_whenTeacherAuthorized() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
        ClinicSession session = createSession(sessionId, teacherId, branchId, LocalDate.now().plusDays(1));
        StudentCourseRecord record = createRecord(recordId, studentId, courseId);
        Course course = createCourse(courseId, teacherId, branchId);

        given(clinicSessionRepository.findByIdAndDeletedAtIsNull(sessionId))
                .willReturn(Optional.of(session));
        given(studentCourseRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(courseRepository.findById(courseId)).willReturn(Optional.of(course));
        given(clinicAttendanceRepository.existsByClinicSessionIdAndStudentCourseRecordId(sessionId, recordId))
                .willReturn(false);
        given(clinicAttendanceRepository.countByClinicSessionId(sessionId)).willReturn(0L);
        given(clinicAttendanceRepository.countOverlappingAttendances(
                recordId,
                session.getDate(),
                session.getStartTime(),
                session.getEndTime()
        )).willReturn(0L);
        given(clinicAttendanceRepository.save(any(ClinicAttendance.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ClinicAttendance attendance = clinicAttendanceService.addAttendance(principal, sessionId, recordId);

        assertThat(attendance.getClinicSessionId()).isEqualTo(sessionId);
        assertThat(attendance.getStudentCourseRecordId()).isEqualTo(recordId);
    }

    @Test
    void requestAttendance_shouldThrow_whenSessionCanceled() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(studentId, MemberRole.STUDENT);
        ClinicSession session = createSession(sessionId, teacherId, branchId, LocalDate.now().plusDays(1));
        session.cancel();

        given(clinicSessionRepository.findByIdAndDeletedAtIsNull(sessionId))
                .willReturn(Optional.of(session));

        assertThatThrownBy(() -> clinicAttendanceService.requestAttendance(principal, sessionId, recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_SESSION_CANCELED);
    }

    @Test
    void moveAttendance_shouldThrow_whenDifferentWeek() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID fromSessionId = UUID.randomUUID();
        UUID toSessionId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(studentId, MemberRole.STUDENT);
        ClinicSession fromSession = createSession(
                fromSessionId,
                teacherId,
                branchId,
                LocalDate.of(2024, Month.MARCH, 4)
        );
        ClinicSession toSession = createSession(
                toSessionId,
                teacherId,
                branchId,
                LocalDate.of(2024, Month.MARCH, 18)
        );
        given(clinicSessionRepository.findByIdAndDeletedAtIsNull(fromSessionId))
                .willReturn(Optional.of(fromSession));
        given(clinicSessionRepository.findByIdAndDeletedAtIsNull(toSessionId))
                .willReturn(Optional.of(toSession));

        assertThatThrownBy(() -> clinicAttendanceService.moveAttendance(principal, fromSessionId, toSessionId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.CLINIC_ATTENDANCE_MOVE_FORBIDDEN);

        verify(clinicAttendanceRepository, never()).save(any());
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

    private StudentCourseRecord createRecord(UUID recordId, UUID studentId, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", recordId);
        return record;
    }

    private Course createCourse(UUID courseId, UUID teacherId, UUID branchId) {
        Course course = Course.create(
                branchId,
                teacherId,
                "Course",
                "Desc",
                LocalDate.of(2024, Month.JANUARY, 1),
                LocalDate.of(2024, Month.DECEMBER, 31),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }
}
