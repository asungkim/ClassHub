package com.classhub.domain.studentprofile.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.course.repository.CourseRepository;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.studentprofile.dto.request.StudentProfileCreateRequest;
import com.classhub.domain.studentprofile.dto.request.StudentProfileUpdateRequest;
import com.classhub.domain.studentprofile.repository.StudentProfileRepository;
import java.util.UUID;
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
class StudentProfileControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebApplicationContext context;

    private Member teacher;
    private Member assistant;
    private Course course;
    @BeforeEach
    void setUp() {
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
                        .name("Course Controller")
                        .company("ClassHub")
                        .teacherId(teacher.getId())
                        .build()
        );
    }

    @Test
    @DisplayName("학생 프로필 생성 API는 201을 응답한다")
    void createStudentProfile() throws Exception {
        StudentProfileCreateRequest request = new StudentProfileCreateRequest(
                course.getId(),
                "Jane Controller",
                "010-9999-1111",
                assistant.getId(),
                "01012345678",
                "Seoul High",
                "1",
                16,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/student-profiles")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.phoneNumber").value("010-9999-1111"));
    }

    @Test
    @DisplayName("학생 프로필 목록을 조회할 수 있다")
    void listStudentProfiles() throws Exception {
        studentProfileRepository.saveAll(
                java.util.List.of(
                        com.classhub.domain.studentprofile.model.StudentProfile.builder()
                                .courseId(course.getId())
                                .teacherId(teacher.getId())
                                .assistantId(assistant.getId())
                                .name("Jane")
                                .phoneNumber("010-9999-0001")
                                .parentPhone("01012345678")
                                .schoolName("Seoul High")
                                .grade("1")
                                .age(15)
                                .build()
                )
        );

        mockMvc.perform(get("/api/v1/student-profiles")
                        .with(teacherPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Jane"));
    }

    @Test
    @DisplayName("학생 프로필을 수정 및 삭제할 수 있다")
    void updateAndDeleteProfile() throws Exception {
        com.classhub.domain.studentprofile.model.StudentProfile profile = studentProfileRepository.save(
                com.classhub.domain.studentprofile.model.StudentProfile.builder()
                        .courseId(course.getId())
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

        StudentProfileUpdateRequest updateRequest = new StudentProfileUpdateRequest(
                "Jane Updated",
                "01098765432",
                "Seoul High",
                "2",
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
                .andExpect(jsonPath("$.data.phoneNumber").value("010-3333-2222"));

        mockMvc.perform(delete("/api/v1/student-profiles/{id}", profile.getId())
                        .with(teacherPrincipal()))
                .andExpect(status().isOk());
    }

    private String toJson(StudentProfileCreateRequest request) {
        return "{"
                + "\"courseId\":" + quote(request.courseId())
                + ",\"name\":" + quote(request.name())
                + ",\"phoneNumber\":" + quote(request.phoneNumber())
                + ",\"assistantId\":" + quote(request.assistantId())
                + ",\"parentPhone\":" + quote(request.parentPhone())
                + ",\"schoolName\":" + quote(request.schoolName())
                + ",\"grade\":" + quote(request.grade())
                + ",\"age\":" + quote(request.age())
                + ",\"memberId\":" + quote(request.memberId())
                + ",\"defaultClinicSlotId\":" + quote(request.defaultClinicSlotId())
                + "}";
    }

    private String toJson(StudentProfileUpdateRequest request) {
        return "{"
                + "\"name\":" + quote(request.name())
                + ",\"parentPhone\":" + quote(request.parentPhone())
                + ",\"schoolName\":" + quote(request.schoolName())
                + ",\"grade\":" + quote(request.grade())
                + ",\"assistantId\":" + quote(request.assistantId())
                + ",\"phoneNumber\":" + quote(request.phoneNumber())
                + ",\"memberId\":" + quote(request.memberId())
                + ",\"defaultClinicSlotId\":" + quote(request.defaultClinicSlotId())
                + ",\"age\":" + quote(request.age())
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
