package com.classhub.domain.member.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.support.RefreshTokenCookieProvider;
import com.classhub.domain.member.application.RegisterService;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.dto.request.RegisterStudentRequest;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.global.response.RsCode;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class MemberControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterService registerService;

    @MockitoBean
    private RefreshTokenCookieProvider refreshTokenCookieProvider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void registerTeacher_shouldReturnTokensAndSetCookie() throws Exception {
        UUID memberId = UUID.randomUUID();
        LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(30);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);
        AuthTokens tokens = new AuthTokens(
                memberId,
                "access-token",
                accessExpiresAt,
                "refresh-token",
                refreshExpiresAt
        );

        given(registerService.registerTeacher(any(RegisterMemberRequest.class))).willReturn(tokens);

        mockMvc.perform(post("/api/v1/members/register/teacher")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterMemberRequest(
                                        "teacher@classhub.com",
                                        "Classhub!1",
                                        "Teacher Kim",
                                        "010-1234-5678"
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()));

        verify(registerService).registerTeacher(any(RegisterMemberRequest.class));
        verify(refreshTokenCookieProvider).setRefreshToken(any(), eq("refresh-token"), eq(refreshExpiresAt));
    }

    @Test
    void registerStudent_shouldReturnTokensAndSetCookie() throws Exception {
        UUID memberId = UUID.randomUUID();
        LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(30);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);
        AuthTokens tokens = new AuthTokens(
                memberId,
                "student-access-token",
                accessExpiresAt,
                "student-refresh-token",
                refreshExpiresAt
        );

        given(registerService.registerStudent(any(RegisterStudentRequest.class))).willReturn(tokens);

        RegisterStudentRequest request = new RegisterStudentRequest(
                new RegisterMemberRequest(
                        "student@classhub.com",
                        "Classhub!1",
                        "Student Hong",
                        "010-2222-3333"
                ),
                "서울중학교",
                StudentGrade.MIDDLE_2,
                java.time.LocalDate.of(2010, 3, 15),
                "010-9999-8888"
        );

        mockMvc.perform(post("/api/v1/members/register/student")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()));

        verify(registerService).registerStudent(any(RegisterStudentRequest.class));
        verify(refreshTokenCookieProvider).setRefreshToken(any(), eq("student-refresh-token"), eq(refreshExpiresAt));
    }
}
