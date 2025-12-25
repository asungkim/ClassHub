package com.classhub.domain.assignment.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.assignment.application.TeacherStudentService;
import com.classhub.domain.assignment.dto.response.TeacherStudentCourseResponse;
import com.classhub.domain.assignment.dto.response.TeacherStudentDetailResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class TeacherStudentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TeacherStudentService teacherStudentService;

    private MockMvc mockMvc;
    private MemberPrincipal principal;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        principal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        authenticationToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
    }

    @Test
    void getTeacherStudents_shouldReturnList() throws Exception {
        UUID studentId = UUID.randomUUID();
        StudentSummaryResponse student = StudentSummaryResponse.builder()
                .memberId(studentId)
                .name("학생")
                .email("student@classhub.com")
                .phoneNumber("01011112222")
                .schoolName("중앙중학교")
                .grade("MIDDLE_2")
                .birthDate(LocalDate.of(2011, 2, 1))
                .age(14)
                .parentPhone("01088887777")
                .build();
        PageResponse<StudentSummaryResponse> response = PageResponse.from(
                new PageImpl<>(List.of(student), PageRequest.of(0, 20), 1)
        );
        given(teacherStudentService.getTeacherStudents(eq(principal), eq(null), eq("학생"), eq(0), eq(20)))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/teacher-students")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken))
                        .param("keyword", "학생")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].memberId").value(studentId.toString()));
    }

    @Test
    void getTeacherStudentDetail_shouldReturnDetail() throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        StudentSummaryResponse student = StudentSummaryResponse.builder()
                .memberId(studentId)
                .name("학생")
                .email("student@classhub.com")
                .phoneNumber("01011112222")
                .schoolName("중앙중학교")
                .grade("MIDDLE_2")
                .birthDate(LocalDate.of(2011, 2, 1))
                .age(14)
                .parentPhone("01088887777")
                .build();
        TeacherStudentCourseResponse course = new TeacherStudentCourseResponse(
                courseId,
                "수학",
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                true,
                assignmentId,
                true,
                recordId
        );
        TeacherStudentDetailResponse response = new TeacherStudentDetailResponse(student, List.of(course));
        given(teacherStudentService.getTeacherStudentDetail(principal, studentId)).willReturn(response);

        mockMvc.perform(get("/api/v1/teacher-students/{studentId}", studentId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.student.memberId").value(studentId.toString()))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.courses[0].assignmentId").value(assignmentId.toString()));
    }
}
