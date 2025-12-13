package com.classhub.domain.personallesson.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.model.CourseSchedule;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.personallesson.dto.request.PersonalLessonCreateRequest;
import com.classhub.domain.personallesson.dto.request.PersonalLessonUpdateRequest;
import com.classhub.domain.personallesson.repository.PersonalLessonRepository;
import com.classhub.domain.studentprofile.application.StudentProfileService;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.response.StudentProfileResponse;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PersonalLessonControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private PersonalLessonRepository personalLessonRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    private Member teacher;
    private Member assistant;
    private Course course;
    private StudentProfileResponse studentProfile;
    @BeforeEach
    void setUp() {
        personalLessonRepository.deleteAll();
        studentProfileRepository.deleteAll();
        courseRepository.deleteAll();
        memberRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters((request, response, chain) -> {
                    Object contextAttr = request.getAttribute(
                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
                    );
                    if (contextAttr instanceof SecurityContext securityContext) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .build();

        teacher = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher Controller")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Assistant Controller")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );
        course = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("Test Course")
                        .company("Test Company")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0)),
                                new CourseSchedule(DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))
                        )))
                        .build()
        );
        studentProfile = studentProfileService.createProfile(
                teacher.getId(),
                new StudentProfileCreateRequest(
                        List.of(course.getId()),
                        "Jane Controller",
                        "010-2222-1111",
                        assistant.getId(),
                        "01012345678",
                        "Seoul High",
                        "1",
                        16,
                        null
                )
        );
    }

    @Test
    @DisplayName("PersonalLesson 생성 및 목록 API 동작 확인")
    void createAndListLessons() throws Exception {
        PersonalLessonCreateRequest request = new PersonalLessonCreateRequest(
                studentProfile.id(),
                LocalDate.of(2025, 1, 1),
                "학습 내용"
        );

        mockMvc.perform(post("/api/v1/personal-lessons")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentProfileId").value(studentProfile.id().toString()));

        mockMvc.perform(get("/api/v1/personal-lessons")
                        .with(teacherPrincipal())
                        .param("studentProfileId", studentProfile.id().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("학습 내용"));
    }

    @Test
    @DisplayName("PersonalLesson 수정/삭제 API 동작 확인")
    void updateAndDeleteLesson() throws Exception {
        PersonalLessonCreateRequest createRequest = new PersonalLessonCreateRequest(
                studentProfile.id(),
                LocalDate.of(2025, 1, 1),
                "학습 내용"
        );

        mockMvc.perform(post("/api/v1/personal-lessons")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated());

        java.util.UUID lessonId = personalLessonRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        PersonalLessonUpdateRequest updateRequest = new PersonalLessonUpdateRequest(
                LocalDate.of(2025, 1, 2),
                "수정된 내용"
        );

        mockMvc.perform(patch("/api/v1/personal-lessons/{lessonId}", lessonId)
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));

        mockMvc.perform(delete("/api/v1/personal-lessons/{lessonId}", lessonId)
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    private String toJson(PersonalLessonCreateRequest request) {
        return "{"
                + "\"studentProfileId\":" + quote(request.studentProfileId())
                + ",\"date\":" + quote(request.date())
                + ",\"content\":" + quote(request.content())
                + "}";
    }

    private String toJson(PersonalLessonUpdateRequest request) {
        return "{"
                + "\"date\":" + quote(request.date())
                + ",\"content\":" + quote(request.content())
                + "}";
    }

    private String quote(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        return "\"" + value.toString() + "\"";
    }

    private RequestPostProcessor teacherPrincipal() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    new MemberPrincipal(teacher.getId()),
                    null
            ));
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            return request;
        };
    }
}
