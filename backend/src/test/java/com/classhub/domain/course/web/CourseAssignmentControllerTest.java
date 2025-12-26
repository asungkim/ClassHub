package com.classhub.domain.course.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.application.CourseAssignmentService;
import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.course.dto.response.CourseScheduleResponse;
import com.classhub.domain.course.dto.response.CourseStudentResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class CourseAssignmentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CourseAssignmentService courseAssignmentService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        authenticationToken = new UsernamePasswordAuthenticationToken(
                teacherPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
    }

    @Test
    void getAssignableCourses_shouldReturnPage() throws Exception {
        CourseResponse response = new CourseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "학원",
                "중3 과학",
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                true,
                List.of(new CourseScheduleResponse(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)))
        );
        PageResponse<CourseResponse> page = new PageResponse<>(
                List.of(response),
                0,
                20,
                1,
                1,
                true,
                true
        );
        given(courseAssignmentService.getAssignableCourses(eq(teacherPrincipal), any(), any(), eq(0), eq(20)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/courses/assignable")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].name").value("중3 과학"));

        verify(courseAssignmentService).getAssignableCourses(eq(teacherPrincipal), any(), any(), eq(0), eq(20));
    }

    @Test
    void getAssignmentCandidates_shouldReturnStudents() throws Exception {
        StudentSummaryResponse summary = StudentSummaryResponse.builder()
                .memberId(UUID.randomUUID())
                .name("학생")
                .email("student@classhub.com")
                .phoneNumber("01000001111")
                .schoolName("중앙중학교")
                .grade("MIDDLE_1")
                .build();
        PageResponse<StudentSummaryResponse> page = new PageResponse<>(
                List.of(summary),
                0,
                20,
                1,
                1,
                true,
                true
        );
        UUID courseId = UUID.randomUUID();
        given(courseAssignmentService.getAssignmentCandidates(eq(teacherPrincipal), eq(courseId), any(), eq(0), eq(20)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/courses/{courseId}/assignment-candidates", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].name").value("학생"));

        verify(courseAssignmentService).getAssignmentCandidates(eq(teacherPrincipal), eq(courseId), any(), eq(0), eq(20));
    }

    @Test
    void getCourseStudents_shouldReturnAssignments() throws Exception {
        StudentSummaryResponse summary = StudentSummaryResponse.builder()
                .memberId(UUID.randomUUID())
                .name("학생")
                .email("student@classhub.com")
                .phoneNumber("01000001111")
                .schoolName("중앙중학교")
                .grade("MIDDLE_1")
                .build();
        CourseStudentResponse item = new CourseStudentResponse(UUID.randomUUID(), false, summary);
        PageResponse<CourseStudentResponse> page = new PageResponse<>(
                List.of(item),
                0,
                20,
                1,
                1,
                true,
                true
        );
        UUID courseId = UUID.randomUUID();
        given(courseAssignmentService.getCourseStudents(eq(teacherPrincipal), eq(courseId), eq(0), eq(20)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/courses/{courseId}/students", courseId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].student.name").value("학생"))
                .andExpect(jsonPath("$.data.content[0].assignmentActive").value(false));

        verify(courseAssignmentService).getCourseStudents(eq(teacherPrincipal), eq(courseId), eq(0), eq(20));
    }
}
