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
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.course.dto.response.CourseResponse;
import com.classhub.domain.enrollment.application.StudentEnrollmentRequestService;
import com.classhub.domain.enrollment.dto.request.StudentEnrollmentRequestCreateRequest;
import com.classhub.domain.enrollment.dto.response.StudentEnrollmentRequestResponse;
import com.classhub.domain.enrollment.model.EnrollmentStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class StudentEnrollmentRequestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentEnrollmentRequestService requestService;

    private MemberPrincipal studentPrincipal;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        studentPrincipal = new MemberPrincipal(UUID.randomUUID(), MemberRole.STUDENT);
        courseResponse = new CourseResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "강남",
                UUID.randomUUID(),
                "러셀",
                "중3 수학",
                "desc",
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                true,
                List.of()
        );
    }

    @Test
    void createRequest_shouldReturnCreatedResponse() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentEnrollmentRequestResponse response = new StudentEnrollmentRequestResponse(
                requestId,
                courseResponse,
                EnrollmentStatus.PENDING,
                "참여하고 싶어요",
                null,
                null,
                null
        );
        given(requestService.createRequest(eq(studentPrincipal.id()), any(StudentEnrollmentRequestCreateRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/student-enrollment-requests")
                        .with(auth())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StudentEnrollmentRequestCreateRequest(courseResponse.courseId(), "참여하고 싶어요")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.requestId").value(requestId.toString()));

        verify(requestService).createRequest(eq(studentPrincipal.id()), any(StudentEnrollmentRequestCreateRequest.class));
    }

    @Test
    void getMyRequests_shouldReturnPagedResponse() throws Exception {
        StudentEnrollmentRequestResponse item = new StudentEnrollmentRequestResponse(
                UUID.randomUUID(),
                courseResponse,
                EnrollmentStatus.PENDING,
                "대기 중",
                null,
                null,
                null
        );
        PageResponse<StudentEnrollmentRequestResponse> page = new PageResponse<>(
                List.of(item),
                0,
                10,
                1,
                1,
                true,
                true
        );
        given(requestService.getMyRequests(eq(studentPrincipal.id()), any(), eq(0), eq(10))).willReturn(page);

        mockMvc.perform(get("/api/v1/student-enrollment-requests/me")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10")
                        .with(auth()))
                .andExpect(status().isOk());

        verify(requestService).getMyRequests(eq(studentPrincipal.id()), any(), eq(0), eq(10));
    }

    @Test
    void cancelRequest_shouldReturnSuccess() throws Exception {
        UUID requestId = UUID.randomUUID();
        StudentEnrollmentRequestResponse response = new StudentEnrollmentRequestResponse(
                requestId,
                courseResponse,
                EnrollmentStatus.CANCELED,
                null,
                null,
                null,
                null
        );
        given(requestService.cancelRequest(studentPrincipal.id(), requestId)).willReturn(response);

        mockMvc.perform(patch("/api/v1/student-enrollment-requests/{id}/cancel", requestId)
                        .with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

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
