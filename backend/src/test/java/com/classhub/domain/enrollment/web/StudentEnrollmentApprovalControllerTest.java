package com.classhub.domain.enrollment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.enrollment.application.StudentEnrollmentApprovalService;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse;
import com.classhub.domain.enrollment.dto.response.TeacherEnrollmentRequestResponse.StudentSummary;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.time.LocalDate;
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
class StudentEnrollmentApprovalControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private StudentEnrollmentApprovalService approvalService;

    private MockMvc mockMvc;
    private MemberPrincipal teacherPrincipal;
    private MemberPrincipal assistantPrincipal;
    private TeacherEnrollmentRequestResponse responseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        assistantPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.ASSISTANT);
        responseDto = new TeacherEnrollmentRequestResponse(
                UUID.randomUUID(),
                new CourseResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "잠실 B102",
                        UUID.randomUUID(),
                        "러셀",
                        "고2 수학",
                        "desc",
                        LocalDate.now(),
                        LocalDate.now().plusMonths(1),
                        true,
                        List.of()
                ),
                new StudentSummary(
                        UUID.randomUUID(),
                        "홍길동",
                        "student@classhub.dev",
                        "010-0000-0000",
                        "ClassHub 고등학교",
                        "HIGH_2",
                        18
                ),
                EnrollmentStatus.PENDING,
                "신청합니다",
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Test
    void getRequestsForTeacher_shouldReturnPage() throws Exception {
        PageResponse<TeacherEnrollmentRequestResponse> page = new PageResponse<>(
                List.of(responseDto),
                0,
                20,
                1,
                1,
                true,
                true
        );
        UUID courseId = UUID.randomUUID();
        given(approvalService.getRequestsForTeacher(
                eq(teacherPrincipal.id()),
                eq(courseId),
                eq(Set.of(EnrollmentStatus.PENDING)),
                eq("hong"),
                eq(0),
                eq(20)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/student-enrollment-requests")
                        .param("courseId", courseId.toString())
                        .param("status", "PENDING")
                        .param("studentName", "hong")
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.content[0].requestId").value(responseDto.requestId().toString()));

        verify(approvalService).getRequestsForTeacher(
                eq(teacherPrincipal.id()),
                eq(courseId),
                eq(Set.of(EnrollmentStatus.PENDING)),
                eq("hong"),
                eq(0),
                eq(20)
        );
    }

    @Test
    void getRequestsForAssistant_shouldReturnPage() throws Exception {
        PageResponse<TeacherEnrollmentRequestResponse> page = new PageResponse<>(
                List.of(responseDto),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(approvalService.getRequestsForAssistant(
                eq(assistantPrincipal.id()),
                eq(null),
                eq(Set.of(EnrollmentStatus.PENDING)),
                eq(null),
                eq(0),
                eq(10)
        )).willReturn(page);

        mockMvc.perform(get("/api/v1/student-enrollment-requests")
                        .param("status", "PENDING")
                        .param("size", "10")
                        .with(auth(assistantPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(approvalService).getRequestsForAssistant(
                eq(assistantPrincipal.id()),
                eq(null),
                eq(Set.of(EnrollmentStatus.PENDING)),
                eq(null),
                eq(0),
                eq(10)
        );
    }

    @Test
    void getRequestDetail_shouldReturnResponse() throws Exception {
        given(approvalService.getRequestDetail(eq(teacherPrincipal.id()), any(UUID.class)))
                .willReturn(responseDto);
        UUID requestId = responseDto.requestId();

        mockMvc.perform(get("/api/v1/student-enrollment-requests/{id}", requestId)
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(requestId.toString()));

        verify(approvalService).getRequestDetail(teacherPrincipal.id(), requestId);
    }

    @Test
    void approveRequest_shouldReturnApproved() throws Exception {
        UUID requestId = responseDto.requestId();
        TeacherEnrollmentRequestResponse approved = new TeacherEnrollmentRequestResponse(
                requestId,
                responseDto.course(),
                responseDto.student(),
                EnrollmentStatus.APPROVED,
                responseDto.studentMessage(),
                LocalDateTime.now(),
                teacherPrincipal.id(),
                responseDto.createdAt()
        );
        given(approvalService.approveRequest(teacherPrincipal.id(), requestId)).willReturn(approved);

        mockMvc.perform(patch("/api/v1/student-enrollment-requests/{id}/approve", requestId)
                        .contentType(APPLICATION_JSON)
                        .content("{}")
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(approvalService).approveRequest(teacherPrincipal.id(), requestId);
    }

    @Test
    void rejectRequest_shouldReturnRejected() throws Exception {
        UUID requestId = responseDto.requestId();
        TeacherEnrollmentRequestResponse rejected = new TeacherEnrollmentRequestResponse(
                requestId,
                responseDto.course(),
                responseDto.student(),
                EnrollmentStatus.REJECTED,
                responseDto.studentMessage(),
                LocalDateTime.now(),
                assistantPrincipal.id(),
                responseDto.createdAt()
        );
        given(approvalService.rejectRequest(assistantPrincipal.id(), requestId)).willReturn(rejected);

        mockMvc.perform(patch("/api/v1/student-enrollment-requests/{id}/reject", requestId)
                        .contentType(APPLICATION_JSON)
                        .content("{}")
                        .with(auth(assistantPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(approvalService).rejectRequest(assistantPrincipal.id(), requestId);
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
