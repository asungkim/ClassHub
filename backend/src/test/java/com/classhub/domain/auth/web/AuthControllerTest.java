package com.classhub.domain.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.request.LogoutRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.MeResponse;
import com.classhub.domain.auth.support.RefreshTokenCookieProvider;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.response.RsCode;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext context;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private RefreshTokenCookieProvider refreshTokenCookieProvider;

        @BeforeEach
        public void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(context)
                                .apply(springSecurity())
                                .build();
        }

        @Test
        void login_shouldReturnTokensAndSetCookie() throws Exception {
                UUID memberId = UUID.randomUUID();
                LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(30);
                LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);
                AuthTokens tokens = new AuthTokens(
                                memberId,
                                "access-token",
                                accessExpiresAt,
                                "refresh-token",
                                refreshExpiresAt);

                given(authService.login(any(LoginRequest.class))).willReturn(tokens);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new LoginRequest("teacher@classhub.com", "Classhub!1"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
                                .andExpect(jsonPath("$.data.accessToken").value("access-token"));

                verify(authService).login(any(LoginRequest.class));
                verify(refreshTokenCookieProvider).setRefreshToken(any(), eq("refresh-token"), eq(refreshExpiresAt));
        }

        @Test
        void refresh_shouldIssueNewTokensFromCookie() throws Exception {
                UUID memberId = UUID.randomUUID();
                LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(15);
                LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(5);
                AuthTokens tokens = new AuthTokens(
                                memberId,
                                "new-access-token",
                                accessExpiresAt,
                                "new-refresh-token",
                                refreshExpiresAt);

                given(refreshTokenCookieProvider.extractRefreshToken(any())).willReturn(Optional.of("cookie-refresh"));
                given(authService.refresh("cookie-refresh")).willReturn(tokens);

                mockMvc.perform(post("/api/v1/auth/refresh"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

                verify(authService).refresh("cookie-refresh");
                verify(refreshTokenCookieProvider).setRefreshToken(any(), eq("new-refresh-token"),
                                eq(refreshExpiresAt));
        }

        @Test
        void me_shouldReturnCurrentMemberData() throws Exception {
                UUID memberId = UUID.randomUUID();
                MemberPrincipal principal = new MemberPrincipal(memberId, MemberRole.TEACHER);
                MeResponse meResponse = new MeResponse(memberId, "teacher@classhub.com", "Teacher Kim",
                                MemberRole.TEACHER);

                given(authService.getCurrentMember(memberId)).willReturn(meResponse);

                mockMvc.perform(get("/api/v1/auth/me")
                                .with(SecurityMockMvcRequestPostProcessors.authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                                principal,
                                                                null,
                                                                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()))
                                .andExpect(jsonPath("$.data.memberId").value(memberId.toString()))
                                .andExpect(jsonPath("$.data.role").value(MemberRole.TEACHER.name()));

                verify(authService).getCurrentMember(memberId);
        }

        @Test
        void logout_shouldUseCookieAndClearIt() throws Exception {
                given(refreshTokenCookieProvider.extractRefreshToken(any()))
                                .willReturn(Optional.of("cookie-refresh-token"));

                mockMvc.perform(post("/api/v1/auth/logout")
                                .contentType(APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(RsCode.SUCCESS.getCode()));

                ArgumentCaptor<LogoutRequest> captor = ArgumentCaptor.forClass(LogoutRequest.class);
                verify(authService).logout(captor.capture());
                assertThat(captor.getValue().refreshToken()).isEqualTo("cookie-refresh-token");
                assertThat(captor.getValue().logoutAll()).isFalse();
                verify(refreshTokenCookieProvider).clearRefreshToken(any());
        }
}
