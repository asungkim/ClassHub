package com.classhub.domain.feedback.web;

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

import com.classhub.domain.feedback.application.FeedbackService;
import com.classhub.domain.feedback.dto.request.FeedbackCreateRequest;
import com.classhub.domain.feedback.dto.response.FeedbackResponse;
import com.classhub.domain.feedback.dto.response.FeedbackWriterResponse;
import com.classhub.domain.feedback.model.FeedbackStatus;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
class FeedbackControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void createFeedback_shouldReturnCreated() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(memberId, MemberRole.TEACHER);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("TEACHER"))
        );
        FeedbackResponse response = new FeedbackResponse(
                UUID.randomUUID(),
                "Great service",
                FeedbackStatus.SUBMITTED,
                LocalDateTime.now(),
                null,
                null,
                new FeedbackWriterResponse(memberId, "Teacher Kim", "teacher@classhub.com", "01012345678",
                        MemberRole.TEACHER)
        );
        given(feedbackService.createFeedback(eq(memberId), any(FeedbackCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/feedback")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FeedbackCreateRequest("Great service"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(RsCode.CREATED.getCode()))
                .andExpect(jsonPath("$.data.content").value("Great service"));

        verify(feedbackService).createFeedback(eq(memberId), any(FeedbackCreateRequest.class));
    }

    @Test
    void getFeedbacksForAdmin_shouldReturnPagedResponse() throws Exception {
        UUID adminId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(adminId, MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        PageResponse<FeedbackResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        given(feedbackService.getFeedbacksForAdmin(eq(FeedbackStatus.SUBMITTED), eq(0), eq(20))).willReturn(page);

        mockMvc.perform(get("/api/v1/feedback")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(feedbackService).getFeedbacksForAdmin(eq(FeedbackStatus.SUBMITTED), eq(0), eq(20));
    }

    @Test
    void getMyFeedbacks_shouldReturnPagedResponse() throws Exception {
        UUID memberId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(memberId, MemberRole.STUDENT);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("STUDENT"))
        );
        PageResponse<FeedbackResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0, true, true);
        given(feedbackService.getMyFeedbacks(eq(memberId), eq(null), eq(0), eq(20))).willReturn(page);

        mockMvc.perform(get("/api/v1/feedback/me")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

        verify(feedbackService).getMyFeedbacks(eq(memberId), eq(null), eq(0), eq(20));
    }

    @Test
    void resolveFeedback_shouldReturnUpdatedResponse() throws Exception {
        UUID adminId = UUID.randomUUID();
        MemberPrincipal principal = new MemberPrincipal(adminId, MemberRole.SUPER_ADMIN);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("SUPER_ADMIN"))
        );
        FeedbackResponse response = new FeedbackResponse(
                UUID.randomUUID(),
                "Need search",
                FeedbackStatus.RESOLVED,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                adminId,
                new FeedbackWriterResponse(UUID.randomUUID(), "Student Lee", "student@classhub.com", "01011112222",
                        MemberRole.STUDENT)
        );
        given(feedbackService.resolveFeedback(any(UUID.class), eq(adminId))).willReturn(response);

        mockMvc.perform(patch("/api/v1/feedback/{feedbackId}/resolve", UUID.randomUUID())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.status").value(FeedbackStatus.RESOLVED.name()));

        verify(feedbackService).resolveFeedback(any(UUID.class), eq(adminId));
    }
}
