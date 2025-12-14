package com.classhub.domain.studentprofile.web;

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
import com.classhub.domain.studentcourseenrollment.model.StudentCourseEnrollment;
import com.classhub.domain.studentcourseenrollment.repository.StudentCourseEnrollmentRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.model.StudentProfile;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class StudentProfileControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentCourseEnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private Member teacher;
    private Member assistant;
    private Course course;
    private Course secondCourse;

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
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
        course = createCourse("Test Course", DayOfWeek.MONDAY);
        secondCourse = createCourse("Second Course", DayOfWeek.WEDNESDAY);
    }

    private Course createCourse(String name, DayOfWeek dayOfWeek) {
        return courseRepository.save(
                Course.builder()
                        .teacherId(teacher.getId())
                        .name(name)
                        .company("Test Company")
                        .schedules(new HashSet<>(Arrays.asList(
                                new CourseSchedule(dayOfWeek, LocalTime.of(14, 0), LocalTime.of(16, 0))
                        )))
                        .build()
        );
    }

    @Test
    @DisplayName("학생 프로필 생성 API는 201을 응답한다")
    void createStudentProfile() throws Exception {
        StudentProfileCreateRequest request = new StudentProfileCreateRequest(
                List.of(course.getId(), secondCourse.getId()),
                "Jane Controller",
                "010-9999-1111",
                assistant.getId(),
                "01012345678",
                "Seoul High",
                "1",
                16,
                null
        );

        mockMvc.perform(post("/api/v1/student-profiles")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-1111"))
                .andExpect(jsonPath("$.data.enrolledCourses.length()").value(2));
    }

    @Test
    @DisplayName("담당 조교 없이 학생 프로필을 생성할 수 있다")
    void createStudentProfile_withoutAssistant() throws Exception {
        StudentProfileCreateRequest request = new StudentProfileCreateRequest(
                List.of(course.getId()),
                "Assistant Free",
                "010-2222-2222",
                null,
                "01012345679",
                "Seoul High",
                "2",
                15,
                null
        );

        mockMvc.perform(post("/api/v1/student-profiles")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assistantId").isEmpty());
    }

    @Test
    @DisplayName("학생 프로필 목록을 조회할 수 있다")
    void listStudentProfiles() throws Exception {
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("Jane")
                        .phoneNumber("010-9999-0001")
                        .parentPhone("01012345678")
                        .schoolName("Seoul High")
                        .grade("1")
                        .age(15)
                        .build()
        );
        enrollmentRepository.save(StudentCourseEnrollment.builder()
                .studentProfileId(profile.getId())
                .courseId(course.getId())
                .teacherId(teacher.getId())
                .build());

        mockMvc.perform(get("/api/v1/student-profiles")
                .with(teacherPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Jane"));
    }

    @Test
    @DisplayName("Assistant는 소속 Teacher의 학생 목록을 조회할 수 있다")
    void listStudentProfiles_asAssistant() throws Exception {
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("Jane")
                        .phoneNumber("010-9999-0001")
                        .parentPhone("01012345678")
                        .schoolName("Seoul High")
                        .grade("1")
                        .age(15)
                        .build()
        );
        enrollmentRepository.save(StudentCourseEnrollment.builder()
                .studentProfileId(profile.getId())
                .courseId(course.getId())
                .teacherId(teacher.getId())
                .build());

        mockMvc.perform(get("/api/v1/student-profiles")
                        .with(assistantPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Jane"));
    }

    @Test
    @DisplayName("학생 프로필을 수정, 비활성화, 다시 활성화할 수 있다")
    void updateAndToggleProfile() throws Exception {
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("Jane")
                        .phoneNumber("010-9999-0001")
                        .parentPhone("01012345678")
                        .schoolName("Seoul High")
                        .grade("1")
                        .age(15)
                        .build()
        );
        enrollmentRepository.save(StudentCourseEnrollment.builder()
                .studentProfileId(profile.getId())
                .courseId(course.getId())
                .teacherId(teacher.getId())
                .build());

        StudentProfileUpdateRequest updateRequest = new StudentProfileUpdateRequest(
                "Jane Updated",
                "01098765432",
                "Seoul High",
                "2",
                List.of(secondCourse.getId()),
                assistant.getId(),
                "010-3333-2222",
                null,
                null,
                17
        );

        mockMvc.perform(patch("/api/v1/student-profiles/{id}", profile.getId())
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneNumber").value("010-3333-2222"))
                .andExpect(jsonPath("$.data.enrolledCourses[0].courseId").value(secondCourse.getId().toString()));

        mockMvc.perform(delete("/api/v1/student-profiles/{id}", profile.getId())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/student-profiles/{id}/activate", profile.getId())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Assistant는 학생 프로필을 수정하거나 삭제할 수 없다")
    void assistantCannotModifyProfile() throws Exception {
        StudentProfile profile = studentProfileRepository.save(
                StudentProfile.builder()
                        .teacherId(teacher.getId())
                        .assistantId(assistant.getId())
                        .name("Jane")
                        .phoneNumber("010-9999-0001")
                        .parentPhone("01012345678")
                        .schoolName("Seoul High")
                        .grade("1")
                        .age(15)
                        .build()
        );
        enrollmentRepository.save(StudentCourseEnrollment.builder()
                .studentProfileId(profile.getId())
                .courseId(course.getId())
                .teacherId(teacher.getId())
                .build());

        StudentProfileUpdateRequest updateRequest = new StudentProfileUpdateRequest(
                "Jane Updated",
                "01098765432",
                "Seoul High",
                "2",
                List.of(secondCourse.getId()),
                assistant.getId(),
                "010-3333-2222",
                null,
                null,
                17
        );

        mockMvc.perform(patch("/api/v1/student-profiles/{id}", profile.getId())
                        .with(assistantPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(delete("/api/v1/student-profiles/{id}", profile.getId())
                        .with(assistantPrincipal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(patch("/api/v1/student-profiles/{id}/activate", profile.getId())
                        .with(assistantPrincipal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    private String toJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private RequestPostProcessor teacherPrincipal() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    new MemberPrincipal(teacher.getId()),
                    null,
                    java.util.List.of(new SimpleGrantedAuthority("TEACHER"))
            ));
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            return request;
        };
    }

    private RequestPostProcessor assistantPrincipal() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                    new MemberPrincipal(assistant.getId()),
                    null,
                    java.util.List.of(new SimpleGrantedAuthority("ASSISTANT"))
            ));
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            return request;
        };
    }
}
