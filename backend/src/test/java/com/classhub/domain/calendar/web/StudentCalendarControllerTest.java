package com.classhub.domain.calendar.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class StudentCalendarControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private SharedLessonRepository sharedLessonRepository;

    @Autowired
    private PersonalLessonRepository personalLessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member teacher;
    private Member assistant;
    private Member student;
    private Course algebra;
    private StudentProfile studentProfile;

    @BeforeEach
    void setUp() {
        personalLessonRepository.deleteAll();
        sharedLessonRepository.deleteAll();
        enrollmentRepository.deleteAll();
        studentProfileRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

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
        student = memberRepository.save(
                Member.builder()
                        .email("student@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Student Lee")
                        .role(MemberRole.STUDENT)
                        .build()
        );

        algebra = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("Algebra")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(15, 0))
                        )))
                        .build()
        );

        StudentProfileResponse profileResponse = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(algebra.getId()),
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
                        .title("공통 진도")
                        .content("교재 10~20쪽")
                        .build()
        );
        personalLessonRepository.save(
                PersonalLesson.builder()
                        .studentProfile(studentProfile)
                        .teacherId(teacher.getId())
                        .writerId(teacher.getId())
                        .date(LocalDate.of(2025, 2, 5))
                        .content("개별 진도")
                        .build()
        );
    }

    @Test
    @DisplayName("인증되지 않은 요청은 401을 반환한다")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        requestCalendar(null, studentProfile.getId(), 2025, 2)
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Student 역할은 접근할 수 없다")
    void shouldReturn403WhenStudentRequests() throws Exception {
        requestCalendar(studentPrincipal(), studentProfile.getId(), 2025, 2)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 month는 400을 반환한다")
    void shouldReturn400WhenMonthInvalid() throws Exception {
        requestCalendar(teacherPrincipal(), studentProfile.getId(), 2025, 13)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 year는 400을 반환한다")
    void shouldReturn400WhenYearInvalid() throws Exception {
        requestCalendar(teacherPrincipal(), studentProfile.getId(), 1999, 2)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Teacher는 학생 캘린더를 조회할 수 있다")
    void shouldReturnCalendarForTeacher() throws Exception {
        requestCalendar(teacherPrincipal(), studentProfile.getId(), 2025, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentId").value(studentProfile.getId().toString()))
                .andExpect(jsonPath("$.data.sharedLessons[0].courseName").value("Algebra"))
                .andExpect(jsonPath("$.data.personalLessons[0].content").value("개별 진도"))
                .andExpect(jsonPath("$.data.clinicRecords").isArray());
    }

    @Test
    @DisplayName("Assistant는 동일 Teacher 소속 학생을 조회할 수 있다")
    void shouldReturnCalendarForAssistant() throws Exception {
        requestCalendar(assistantPrincipal(), studentProfile.getId(), 2025, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sharedLessons").isArray())
                .andExpect(jsonPath("$.data.personalLessons").isArray());
    }

    @Test
    @DisplayName("데이터가 없는 월은 빈 배열을 반환한다")
    void shouldReturnEmptyListsWhenNoData() throws Exception {
        requestCalendar(teacherPrincipal(), studentProfile.getId(), 2025, 12)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharedLessons").isEmpty())
                .andExpect(jsonPath("$.data.personalLessons").isEmpty())
                .andExpect(jsonPath("$.data.clinicRecords").isEmpty());
    }

    private ResultActions requestCalendar(RequestPostProcessor principal, UUID studentId, int year, int month)
            throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/v1/students/{studentId}/calendar", studentId)
                .param("year", String.valueOf(year))
                .param("month", String.valueOf(month));
        if (principal != null) {
            builder.with(principal);
        }
        return mockMvc.perform(builder);
    }

    private RequestPostProcessor teacherPrincipal() {
        return authenticationToken(teacher.getId(), "TEACHER");
    }

    private RequestPostProcessor assistantPrincipal() {
        return authenticationToken(assistant.getId(), "ASSISTANT");
    }

    private RequestPostProcessor studentPrincipal() {
        return authenticationToken(student.getId(), "STUDENT");
    }

    private RequestPostProcessor authenticationToken(UUID memberId, String authority) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(
                new MemberPrincipal(memberId),
                null,
                authority
        );
        token.setAuthenticated(true);
        return authentication(token);
    }
}
