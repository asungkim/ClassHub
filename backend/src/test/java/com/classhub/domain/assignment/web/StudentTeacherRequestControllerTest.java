package com.classhub.domain.assignment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.assignment.application.StudentTeacherRequestService;
import com.classhub.domain.assignment.dto.request.StudentTeacherRequestCreateRequest;
import com.classhub.domain.assignment.dto.response.StudentTeacherRequestResponse;
import com.classhub.domain.assignment.model.TeacherStudentRequestStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.response.StudentSummaryResponse;
import com.classhub.domain.member.dto.response.TeacherSearchResponse;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
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
class StudentTeacherRequestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentTeacherRequestService requestService;

    private MemberPrincipal studentPrincipal;
    private MemberPrincipal teacherPrincipal;
    private TeacherSearchResponse teacherResponse;
    private StudentSummaryResponse studentSummary;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
        teacherPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.TEACHER);
        teacherResponse = new TeacherSearchResponse(
                UUID.randomUUID(),
                "Teacher Kim",
                List.of()
        );
        studentSummary = StudentSummaryResponse.builder()
                .memberId(studentPrincipal.id())
                .name("학생")
                .email("student@classhub.com")
                .phoneNumber("01000001111")
                .schoolName("중앙중학교")
                .grade("MIDDLE_1")
                .build();
    }

    @Test
    void createRequest_shouldReturnCreatedResponse() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequestResponse response = new StudentTeacherRequestResponse(
                requestId,
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.PENDING,
                "요청합니다",
                null,
                null,
                null
        );
        given(requestService.createRequest(eq(studentPrincipal.id()), any(StudentTeacherRequestCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/teacher-student-requests")
                        .with(auth(studentPrincipal))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StudentTeacherRequestCreateRequest(teacherResponse.teacherId(), "요청합니다")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.requestId").value(requestId.toString()));

        verify(requestService).createRequest(eq(studentPrincipal.id()), any(StudentTeacherRequestCreateRequest.class));
    }

    @Test
    void getMyRequests_shouldReturnPagedResponse() throws Exception {
        StudentTeacherRequestResponse item = new StudentTeacherRequestResponse(
                UUID.randomUUID(),
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.PENDING,
                "대기 중",
                null,
                null,
                null
        );
        PageResponse<StudentTeacherRequestResponse> page = new PageResponse<>(
                List.of(item),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(requestService.getMyRequests(eq(studentPrincipal.id()), any(), eq(0), eq(10)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/teacher-student-requests")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .with(auth(studentPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(requestService).getMyRequests(eq(studentPrincipal.id()), any(), eq(0), eq(10));
    }

    @Test
    void cancelRequest_shouldReturnSuccess() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequestResponse response = new StudentTeacherRequestResponse(
                requestId,
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.CANCELLED,
                null,
                null,
                null,
                null
        );
        given(requestService.cancelRequest(studentPrincipal.id(), requestId)).willReturn(response);

        mockMvc.perform(patch("/api/v1/teacher-student-requests/{id}/cancel", requestId)
                        .with(auth(studentPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(requestService).cancelRequest(studentPrincipal.id(), requestId);
    }

    @Test
    void getRequests_shouldReturnTeacherInboxForTeacherRole() throws Exception {
        StudentTeacherRequestResponse item = new StudentTeacherRequestResponse(
                UUID.randomUUID(),
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.PENDING,
                "요청",
                null,
                null,
                null
        );
        PageResponse<StudentTeacherRequestResponse> page = new PageResponse<>(
                List.of(item),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(requestService.getRequestsForTeacher(eq(teacherPrincipal.id()), any(), any(), eq(0), eq(10)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/teacher-student-requests")
                        .param("status", "PENDING")
                        .param("keyword", "학생")
                        .param("page", "0")
                        .param("size", "10")
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(requestService).getRequestsForTeacher(eq(teacherPrincipal.id()), any(), eq("학생"), eq(0), eq(10));
    }

    @Test
    void approveRequest_shouldReturnSuccessForTeacher() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequestResponse response = new StudentTeacherRequestResponse(
                requestId,
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.APPROVED,
                "요청합니다",
                null,
                teacherPrincipal.id(),
                null
        );
        given(requestService.approveRequest(teacherPrincipal.id(), requestId)).willReturn(response);

        mockMvc.perform(patch("/api/v1/teacher-student-requests/{id}/approve", requestId)
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(requestService).approveRequest(teacherPrincipal.id(), requestId);
    }

    @Test
    void rejectRequest_shouldReturnSuccessForTeacher() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequestResponse response = new StudentTeacherRequestResponse(
                requestId,
                teacherResponse,
                studentSummary,
                TeacherStudentRequestStatus.REJECTED,
                "요청합니다",
                null,
                teacherPrincipal.id(),
                null
        );
        given(requestService.rejectRequest(teacherPrincipal.id(), requestId)).willReturn(response);

        mockMvc.perform(patch("/api/v1/teacher-student-requests/{id}/reject", requestId)
                        .with(auth(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(requestService).rejectRequest(teacherPrincipal.id(), requestId);
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
