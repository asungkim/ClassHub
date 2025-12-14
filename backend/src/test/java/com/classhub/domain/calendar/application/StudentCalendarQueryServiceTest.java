package com.classhub.domain.calendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.calendar.dto.response.StudentCalendarResponse;
import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.personallesson.model.PersonalLesson;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.sharedlesson.model.SharedLesson;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.application.StudentProfileService;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StudentCalendarQueryServiceTest {

    @Autowired
    private StudentCalendarQueryService studentCalendarQueryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentCourseEnrollmentRepository studentCourseEnrollmentRepository;

    @Autowired
    private SharedLessonRepository sharedLessonRepository;

    @Autowired
    private PersonalLessonRepository personalLessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;
    private Member otherTeacher;
    private Course algebra;
    private Course biology;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() {
        personalLessonRepository.deleteAll();
        sharedLessonRepository.deleteAll();
        studentCourseEnrollmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Kim")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Park")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );
        otherTeacher = memberRepository.save(
                Member.builder()
                        .email("other@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Choi")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        algebra = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("Algebra")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0)),
                                new CourseSchedule(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0))
                        )))
                        .build()
        );
        biology = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("Biology")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.TUESDAY, LocalTime.of(16, 0), LocalTime.of(17, 0)),
                                new CourseSchedule(DayOfWeek.THURSDAY, LocalTime.of(16, 0), LocalTime.of(17, 0))
                        )))
                        .build()
        );

        StudentProfileResponse profileResponse = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(algebra.getId(), biology.getId()),
                        "학생 A",
                        "010-0000-0000",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        15,
                        null
                )
        );

        studentProfile = studentProfileRepository.findById(profileResponse.id())
                .orElseThrow(() -> new IllegalStateException("student profile missing"));

        sharedLessonRepository.save(
                SharedLesson.builder()
                        .course(algebra)
                        .writerId(teacher.getId())
                        .date(LocalDate.of(2025, 2, 3))
                        .title("공통 진도 A")
                        .content("교재 10~15쪽")
                        .build()
        );
        sharedLessonRepository.save(
                SharedLesson.builder()
                        .course(biology)
                        .writerId(teacher.getId())
                        .date(LocalDate.of(2025, 2, 3))
                        .title("공통 진도 B")
                        .content("실험 준비")
                        .build()
        );

        personalLessonRepository.save(
                PersonalLesson.builder()
                        .studentProfile(studentProfile)
                        .teacherId(teacher.getId())
                        .writerId(teacher.getId())
                        .date(LocalDate.of(2025, 2, 5))
                        .content("개별 클리닉 메모")
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 학생 월간 캘린더를 조회할 수 있다")
    void shouldReturnCalendarForTeacher() {
        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2025,
                2
        );

        assertThat(response.schemaVersion()).isEqualTo(1);
        assertThat(response.sharedLessons()).hasSize(2);
        assertThat(response.sharedLessons().getFirst().courseName()).isEqualTo("Algebra");
        assertThat(response.personalLessons()).hasSize(1);
        assertThat(response.personalLessons().getFirst().content()).isEqualTo("개별 클리닉 메모");
        assertThat(response.clinicRecords()).isEmpty();
    }

    @Test
    @DisplayName("다른 Teacher 소속 Assistant는 접근할 수 없다")
    void shouldDenyAssistantFromOtherTeacher() {
        Member foreignAssistant = memberRepository.save(
                Member.builder()
                        .email("foreign-assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Han")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(otherTeacher.getId())
                        .build()
        );

        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                foreignAssistant.getId(),
                studentProfile.getId(),
                2025,
                2
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.FORBIDDEN);
    }

    @Test
    @DisplayName("StudentProfile을 찾을 수 없으면 STUDENT_PROFILE_NOT_FOUND 예외")
    void shouldThrowWhenStudentNotFound() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                UUID.randomUUID(), // 존재하지 않는 학생
                2025,
                2
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.STUDENT_PROFILE_NOT_FOUND);
    }

    @Test
    @DisplayName("해당 월에 데이터가 없으면 빈 리스트 반환")
    void shouldReturnEmptyLists_whenNoLessonsInMonth() {
        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2025,
                12 // 데이터 없는 달
        );

        assertThat(response.sharedLessons()).isEmpty();
        assertThat(response.personalLessons()).isEmpty();
        assertThat(response.clinicRecords()).isEmpty();
    }

    @Test
    @DisplayName("다른 Teacher는 접근할 수 없다")
    void shouldThrowWhenTeacherHasNoAccess() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                otherTeacher.getId(),
                studentProfile.getId(),
                2025,
                2
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.FORBIDDEN);
    }

    @Test
    @DisplayName("Assistant는 본인 Teacher의 학생에 접근 가능")
    void shouldAllowAssistant_whenBelongsToSameTeacher() {
        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                assistant.getId(),
                studentProfile.getId(),
                2025,
                2
        );

        assertThat(response.sharedLessons()).hasSize(2);
        assertThat(response.personalLessons()).hasSize(1);
    }

    @Test
    @DisplayName("윤년 2월 처리")
    void shouldHandleLeapYearFebruary_when2024() {
        sharedLessonRepository.save(
                SharedLesson.builder()
                        .course(algebra)
                        .writerId(teacher.getId())
                        .date(LocalDate.of(2024, 2, 29)) // 윤년
                        .title("윤년 진도")
                        .content("2월 29일")
                        .build()
        );

        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2024,
                2
        );

        assertThat(response.sharedLessons())
                .anyMatch(lesson -> lesson.date().equals(LocalDate.of(2024, 2, 29)));
    }

    @Test
    @DisplayName("year 범위 검증 - MIN_YEAR 미만")
    void shouldThrowWhenYearBelowMin() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                1999, // MIN_YEAR(2000) 미만
                2
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("year 범위 검증 - MAX_YEAR 초과")
    void shouldThrowWhenYearAboveMax() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2101, // MAX_YEAR(2100) 초과
                2
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("month 범위 검증 - 0")
    void shouldThrowWhenMonthIsZero() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2025,
                0 // 유효하지 않은 월
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("month 범위 검증 - 13")
    void shouldThrowWhenMonthIs13() {
        assertThatThrownBy(() -> studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2025,
                13 // 유효하지 않은 월
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("rsCode")
                .isEqualTo(RsCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("학생이 여러 Course에 속할 때 모든 SharedLesson 조회")
    void shouldHandleMultipleCourses_whenStudentEnrolledInThree() {
        // 이미 algebra, biology 2개 Course에 속해있음
        long enrollmentCount = studentCourseEnrollmentRepository
                .findAllCourseIdsByStudentProfileId(studentProfile.getId())
                .size();
        assertThat(enrollmentCount).isEqualTo(2);

        StudentCalendarResponse response = studentCalendarQueryService.getMonthlyCalendar(
                teacher.getId(),
                studentProfile.getId(),
                2025,
                2
        );

        // algebra + biology 각 1개씩 총 2개
        assertThat(response.sharedLessons()).hasSize(2);
        assertThat(response.sharedLessons())
                .extracting("courseName")
                .containsExactlyInAnyOrder("Algebra", "Biology");
    }
}
