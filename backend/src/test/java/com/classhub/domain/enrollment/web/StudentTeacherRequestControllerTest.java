package com.classhub.domain.enrollment.web;

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

import com.classhub.domain.enrollment.application.StudentTeacherRequestService;
import com.classhub.domain.enrollment.dto.request.StudentTeacherRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentTeacherRequestResponse;
import com.classhub.domain.enrollment.model.TeacherStudentRequestStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
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
    private TeacherSearchResponse teacherResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
        teacherResponse = new TeacherSearchResponse(
                UUID.randomUUID(),
                "Teacher Kim",
                List.of()
        );
    }

    @Test
    void createRequest_shouldReturnCreatedResponse() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentTeacherRequestResponse response = new StudentTeacherRequestResponse(
                requestId,
                teacherResponse,
                TeacherStudentRequestStatus.PENDING,
                "요청합니다",
                null,
                null,
                null
        );
        given(requestService.createRequest(eq(studentPrincipal.id()), any(StudentTeacherRequestCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/teacher-student-requests")
                        .with(auth())
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
                        .with(auth()))
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
                TeacherStudentRequestStatus.CANCELLED,
                null,
                null,
                null,
                null
        );
        given(requestService.cancelRequest(studentPrincipal.id(), requestId)).willReturn(response);

        mockMvc.perform(patch("/api/v1/teacher-student-requests/{id}/cancel", requestId)
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(requestService).cancelRequest(studentPrincipal.id(), requestId);
    }

    private RequestPostProcessor auth() {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        studentPrincipal,
                        null,
                        List.of(new SimpleGrantedAuthority(studentPrincipal.role().name()))
                )
        );
    }
}
