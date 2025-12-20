package com.classhub.domain.enrollment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.enrollment.application.StudentEnrollmentAdminService;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse.StudentSummary;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AdminStudentEnrollmentControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentEnrollmentAdminService adminService;

    private MockMvc mockMvc;
    private MemberPrincipal adminPrincipal;
    private TeacherEnrollmentRequestResponse responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        adminPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.SUPER_ADMIN);
        responseDto = new TeacherEnrollmentRequestResponse(
                UUID.randomUUID(),
                null,
                new StudentSummary(
                        UUID.randomUUID(),
                        "홍길동",
                        "student@classhub.dev",
                        "010-0000-0000",
                        "ClassHub",
                        "HIGH_2",
                        18
                ),
                EnrollmentStatus.PENDING,
                "참여",
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Test
    void getRequests_shouldReturnPage() throws Exception {
        PageResponse<TeacherEnrollmentRequestResponse> page = new PageResponse<>(
                List.of(responseDto),
                0,
                20,
                1,
                1,
                true,
                true
        );
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        given(adminService.getRequests(eq(teacherId), eq(courseId), eq(Set.of(EnrollmentStatus.PENDING)), eq("hong"), eq(0), eq(20)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/admin/student-enrollment-requests")
                        .param("teacherId", teacherId.toString())
                        .param("courseId", courseId.toString())
                        .param("status", "PENDING")
                        .param("studentName", "hong")
                        .with(auth(adminPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].student.name").value("홍길동"));

        verify(adminService).getRequests(
                eq(teacherId),
                eq(courseId),
                eq(Set.of(EnrollmentStatus.PENDING)),
                eq("hong"),
                eq(0),
                eq(20)
        );
    }

    private RequestPostProcessor auth(MemberPrincipal principal) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(principal.role().name()))
                )
        );
    }
}
