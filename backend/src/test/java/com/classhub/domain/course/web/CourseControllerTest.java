package com.classhub.domain.course.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
class CourseControllerTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private Member teacher;
    private Member assistant;

    @BeforeEach
    void setUp() {
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
                        .password("encoded")
                        .name("Teacher")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assistant = memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password("encoded")
                        .name("Assistant")
                        .role(MemberRole.ASSISTANT)
                        .teacherId(teacher.getId())
                        .build()
        );
    }

    @Test
    @DisplayName("Teacher는 Course를 생성할 수 있다")
    void createCourse_success() throws Exception {
        String payload = """
                {
                  "name": "중등 수학 A반",
                  "company": "ABC 학원",
                  "daysOfWeek": ["MONDAY", "FRIDAY"],
                  "startTime": "14:00",
                  "endTime": "16:00"
                }
                """;

        mockMvc.perform(post("/api/v1/courses")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("중등 수학 A반"))
                .andExpect(jsonPath("$.data.teacherId").value(teacher.getId().toString()));
    }

    @Test
    @DisplayName("요일이 비어 있으면 Course 생성 요청은 400을 반환한다")
    void createCourse_shouldReturnBadRequest_whenDaysEmpty() throws Exception {
        String payload = """
                {
                  "name": "중등 수학 A반",
                  "company": "ABC 학원",
                  "daysOfWeek": [],
                  "startTime": "14:00",
                  "endTime": "16:00"
                }
                """;

        mockMvc.perform(post("/api/v1/courses")
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Teacher는 활성 상태로 Course 목록을 필터링할 수 있다")
    void listCourses_withActiveFilter() throws Exception {
        Course active = createCourse("활성 반", teacher.getId());
        Course inactive = createCourse("비활성 반", teacher.getId());
        inactive.deactivate();
        courseRepository.save(inactive);

        mockMvc.perform(get("/api/v1/courses")
                        .with(teacherPrincipal())
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("활성 반"));
    }

    @Test
    @DisplayName("Teacher 소유가 아니면 Course 비활성화 요청이 거부된다")
    void deactivateCourse_forbiddenWhenNotOwner() throws Exception {
        Course course = createCourse("중등 수학 A반", teacher.getId());

        mockMvc.perform(patch("/api/v1/courses/{courseId}/deactivate", course.getId())
                        .with(assistantPrincipal()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("종료 시간이 시작보다 이르면 수정 요청은 400을 반환한다")
    void updateCourse_shouldReturnBadRequest_whenEndBeforeStart() throws Exception {
        Course course = createCourse("중등 수학 A반", teacher.getId());
        String payload = """
                {
                  "startTime": "16:00",
                  "endTime": "14:00"
                }
                """;

        mockMvc.perform(patch("/api/v1/courses/{courseId}", course.getId())
                        .with(teacherPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    private Course createCourse(String name, UUID teacherId) {
        Course course = Course.builder()
                .name(name)
                .company("ABC 학원")
                .teacherId(teacherId)
                .daysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        return courseRepository.save(course);
    }

    private RequestPostProcessor teacherPrincipal() {
        return principal(teacher.getId(), "TEACHER");
    }

    private RequestPostProcessor assistantPrincipal() {
        return principal(assistant.getId(), "ASSISTANT");
    }

    private RequestPostProcessor principal(UUID memberId, String authority) {
        return request -> {
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(new TestingAuthenticationToken(
                    new MemberPrincipal(memberId),
                    null,
                    List.of(new SimpleGrantedAuthority(authority))
            ));
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );
            return request;
        };
    }
}
