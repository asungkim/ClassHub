package com.classhub.domain.sharedlesson.web;

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
import com.classhub.domain.sharedlesson.dto.request.SharedLessonCreateRequest;
import com.classhub.domain.sharedlesson.dto.request.SharedLessonUpdateRequest;
import com.classhub.domain.sharedlesson.repository.SharedLessonRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class SharedLessonControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private SharedLessonRepository sharedLessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    private Member teacher;
    private Member anotherTeacher;
    private Course course;

    @BeforeEach
    void setUp() {
        sharedLessonRepository.deleteAll();
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
        anotherTeacher = memberRepository.save(
                Member.builder()
                        .email("teacher2@classhub.com")
                        .password(passwordEncoder.encode("Classhub!1"))
                        .name("Teacher B")
                        .role(MemberRole.TEACHER)
                        .build()
        );
        course = courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name("공통 진도반")
                        .company("ClassHub")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
                        )))
                        .build()
        );
    }

    @Test
    @DisplayName("SharedLesson 생성 및 목록 조회")
    void createAndListSharedLessons() throws Exception {
        SharedLessonCreateRequest createRequest = new SharedLessonCreateRequest(
                course.getId(),
                LocalDate.of(2025, 1, 1),
                "1주차",
                "교재 10~20p"
        );

        mockMvc.perform(post("/api/v1/shared-lessons")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("1주차"));

        mockMvc.perform(get("/api/v1/shared-lessons")
                        .with(teacherPrincipal())
                        .param("courseId", course.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("1주차"));
    }

    @Test
    @DisplayName("SharedLesson 수정과 삭제")
    void updateAndDeleteSharedLesson() throws Exception {
        SharedLessonCreateRequest createRequest = new SharedLessonCreateRequest(
                course.getId(),
                LocalDate.now(),
                "1주차",
                "내용"
        );

        mockMvc.perform(post("/api/v1/shared-lessons")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated());

        UUID lessonId = sharedLessonRepository.findAll().stream().findFirst().orElseThrow().getId();

        SharedLessonUpdateRequest updateRequest = new SharedLessonUpdateRequest(
                LocalDate.now().plusDays(1),
                "수정된 제목",
                "수정된 내용"
        );

        mockMvc.perform(patch("/api/v1/shared-lessons/{lessonId}", lessonId)
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));

        mockMvc.perform(delete("/api/v1/shared-lessons/{lessonId}", lessonId)
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SharedLesson 생성 시 검증 실패")
    void shouldFailValidation() throws Exception {
        SharedLessonCreateRequest request = new SharedLessonCreateRequest(
                course.getId(),
                LocalDate.now(),
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/shared-lessons")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 Teacher가 Course에 접근하면 403을 반환한다")
    void shouldReturnForbiddenForOtherTeacher() throws Exception {
        SharedLessonCreateRequest request = new SharedLessonCreateRequest(
                course.getId(),
                LocalDate.now(),
                "무단 생성",
                "내용"
        );

        mockMvc.perform(post("/api/v1/shared-lessons")
                        .with(otherTeacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor teacherPrincipal() {
        return request -> {
            SecurityContext contextHolder = SecurityContextHolder.createEmptyContext();
            contextHolder.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    new MemberPrincipal(teacher.getId()),
                    null,
                    java.util.List.of(() -> "TEACHER")
            ));
            request.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contextHolder);
            return request;
        };
    }

    private RequestPostProcessor otherTeacherPrincipal() {
        return request -> {
            SecurityContext contextHolder = SecurityContextHolder.createEmptyContext();
            contextHolder.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    new MemberPrincipal(anotherTeacher.getId()),
                    null,
                    java.util.List.of(() -> "TEACHER")
            ));
            request.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contextHolder);
            return request;
        };
    }

    private String toJson(SharedLessonCreateRequest request) {
        return "{"
                + "\"courseId\":\"" + request.courseId() + "\""
                + ",\"date\":" + (request.date() != null ? "\"" + request.date() + "\"" : null)
                + ",\"title\":" + quote(request.title())
                + ",\"content\":" + quote(request.content())
                + "}";
    }

    private String toJson(SharedLessonUpdateRequest request) {
        return "{"
                + "\"date\":" + (request.date() != null ? "\"" + request.date() + "\"" : null)
                + ",\"title\":" + quote(request.title())
                + ",\"content\":" + quote(request.content())
                + "}";
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }
}
