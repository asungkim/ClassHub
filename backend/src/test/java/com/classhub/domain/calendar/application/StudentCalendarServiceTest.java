package com.classhub.domain.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.calendar.mapper.StudentCalendarMapper;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceEventProjection;
import com.classhub.domain.clinic.clinicattendance.repository.ClinicAttendanceRepository;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.progress.course.model.CourseProgress;
import com.classhub.domain.progress.course.repository.CourseProgressRepository;
import com.classhub.domain.calendar.dto.StudentCalendarResponse;
import com.classhub.domain.progress.personal.model.PersonalProgress;
import com.classhub.domain.progress.personal.repository.PersonalProgressRepository;
import com.classhub.domain.progress.support.ProgressPermissionValidator;
import com.classhub.domain.studentcourse.model.StudentCourseRecord;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentCalendarServiceTest {

    @Mock
    private ProgressPermissionValidator permissionValidator;
    @Mock
    private CourseProgressRepository courseProgressRepository;
    @Mock
    private PersonalProgressRepository personalProgressRepository;
    @Mock
    private ClinicAttendanceRepository clinicAttendanceRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private MemberRepository memberRepository;

    @Spy
    private StudentCalendarMapper studentCalendarMapper = new StudentCalendarMapper();

    @InjectMocks
    private StudentCalendarService studentCalendarService;

    private MemberPrincipal teacherPrincipal;
    private UUID teacherId;
    private UUID assistantId;
    private UUID studentId;
    private UUID courseId;
    private UUID recordId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        recordId = UUID.randomUUID();
        teacherPrincipal = new MemberPrincipal(teacherId, MemberRole.TEACHER);
    }

    @Test
    void getStudentCalendar_shouldAggregateEvents() {
        YearMonth target = YearMonth.now();
        StudentCourseRecord record = createRecord(recordId, studentId, courseId);
        given(permissionValidator.ensureCalendarAccess(teacherPrincipal, studentId))
                .willReturn(List.of(record));

        CourseProgress courseProgress = CourseProgress.builder()
                .courseId(courseId)
                .writerId(teacherId)
                .date(target.atDay(2))
                .title("Course")
                .content("memo")
                .build();
        ReflectionTestUtils.setField(courseProgress, "id", UUID.randomUUID());

        PersonalProgress personalProgress = PersonalProgress.builder()
                .studentCourseRecordId(recordId)
                .writerId(teacherId)
                .date(target.atDay(3))
                .title("Personal")
                .content("memo")
                .build();
        ReflectionTestUtils.setField(personalProgress, "id", UUID.randomUUID());

        ClinicAttendanceEventProjection clinicEvent = new TestClinicEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                courseId,
                UUID.randomUUID(),
                target.atDay(4),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                false,
                UUID.randomUUID(),
                "Clinic",
                assistantId
        );

        given(courseProgressRepository.findByCourseIdsAndDateRange(any(), any(), any()))
                .willReturn(List.of(courseProgress));
        given(personalProgressRepository.findByRecordIdsAndDateRange(any(), any(), any()))
                .willReturn(List.of(personalProgress));
        given(clinicAttendanceRepository.findEventsByRecordIdsAndDateRange(any(), any(), any()))
                .willReturn(List.of(clinicEvent));

        Course course = createCourse(courseId, teacherId, "중3 수학");
        given(courseRepository.findAllById(any())).willReturn(List.of(course));

        Member teacher = createMember(teacherId, MemberRole.TEACHER);
        Member assistant = createMember(assistantId, MemberRole.ASSISTANT);
        given(memberRepository.findAllById(any())).willReturn(List.of(teacher, assistant));

        StudentCalendarResponse response = studentCalendarService.getStudentCalendar(
                teacherPrincipal,
                studentId,
                target.getYear(),
                target.getMonthValue()
        );

        assertThat(response.courseProgress()).hasSize(1);
        assertThat(response.courseProgress().get(0).courseName()).isEqualTo("중3 수학");
        assertThat(response.personalProgress()).hasSize(1);
        assertThat(response.personalProgress().get(0).courseName()).isEqualTo("중3 수학");
        assertThat(response.clinicEvents()).hasSize(1);
        assertThat(response.clinicEvents().get(0).recordSummary().writerRole())
                .isEqualTo(MemberRole.ASSISTANT);
    }

    @Test
    void getStudentCalendar_shouldRejectOutOfRangeMonth() {
        YearMonth target = YearMonth.now().plusMonths(4);

        assertThatThrownBy(() -> studentCalendarService.getStudentCalendar(
                teacherPrincipal,
                studentId,
                target.getYear(),
                target.getMonthValue()
        ))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.BAD_REQUEST);
    }

    private StudentCourseRecord createRecord(UUID recordId, UUID studentId, UUID courseId) {
        StudentCourseRecord record = StudentCourseRecord.create(studentId, courseId, null, null, null);
        ReflectionTestUtils.setField(record, "id", recordId);
        return record;
    }

    private Course createCourse(UUID courseId, UUID teacherId, String name) {
        Course course = Course.create(
                UUID.randomUUID(),
                teacherId,
                name,
                "Desc",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30),
                Set.of()
        );
        ReflectionTestUtils.setField(course, "id", courseId);
        return course;
    }

    private Member createMember(UUID memberId, MemberRole role) {
        Member member = Member.builder()
                .email(role.name().toLowerCase() + "@classhub.dev")
                .password("encoded")
                .name(role.name())
                .phoneNumber("01000000000")
                .role(role)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private record TestClinicEvent(
            UUID clinicSessionId,
            UUID clinicAttendanceId,
            UUID courseId,
            UUID slotId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            boolean canceled,
            UUID recordId,
            String recordTitle,
            UUID recordWriterId
    ) implements ClinicAttendanceEventProjection {
        @Override
        public UUID getClinicSessionId() {
            return clinicSessionId;
        }

        @Override
        public UUID getClinicAttendanceId() {
            return clinicAttendanceId;
        }

        @Override
        public UUID getCourseId() {
            return courseId;
        }

        @Override
        public UUID getSlotId() {
            return slotId;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public LocalTime getStartTime() {
            return startTime;
        }

        @Override
        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public UUID getRecordId() {
            return recordId;
        }

        @Override
        public String getRecordTitle() {
            return recordTitle;
        }

        @Override
        public UUID getRecordWriterId() {
            return recordWriterId;
        }
    }
}
