package com.classhub.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.global.jwt.JwtProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Spring Security Config PLAN의 요구사항을 통합 테스트로 검증한다.
 * - /auth/** 는 permitAll
 * - 기타 엔드포인트는 인증 필요 (없으면 401)
 * - /api/admin/** 는 SUPERADMIN 권한만 접근 가능 (권한 없으면 403)
 */
@SpringBootTest(properties = "JWT_SECRET_KEY=0123456789012345678901234567890123456789012345678901234567890123")
@Import({
        SecurityIntegrationTest.AuthController.class,
        SecurityIntegrationTest.SecureController.class,
        SecurityIntegrationTest.AdminController.class
})
class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("permitAll 경로는 토큰 없이 접근된다")
    void authEndpointIsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("login-ok"));
    }

    @Test
    @DisplayName("보호된 경로는 토큰이 없으면 401을 응답한다")
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/secure/ping")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 토큰이 있으면 보호된 경로에 접근할 수 있다")
    void protectedEndpointWithTokenReturnsOk() throws Exception {
        String token = jwtProvider.generateAccessToken(UUID.randomUUID(), "TEACHER");

        mockMvc.perform(get("/api/v1/secure/ping")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("secure-ok"));
    }

    @Test
    @DisplayName("/api/admin/** 경로는 SUPERADMIN 권한이 아니면 403을 응답한다")
    void adminEndpointRequiresSuperAdminAuthority() throws Exception {
        String teacherToken = jwtProvider.generateAccessToken(UUID.randomUUID(), "TEACHER");

        mockMvc.perform(get("/api/admin/panel")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        String adminToken = jwtProvider.generateAccessToken(UUID.randomUUID(), "SUPERADMIN");
        mockMvc.perform(get("/api/admin/panel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @RestController
    static class AuthController {
        @GetMapping("/auth/login")
        public String login() {
            return "login-ok";
        }
    }

    @RestController
    static class SecureController {
        @GetMapping("/api/v1/secure/ping")
        public String securePing() {
            return "secure-ok";
        }
    }

    @RestController
    static class AdminController {
        @GetMapping("/api/admin/panel")
        public String admin() {
            return "admin-ok";
        }
    }
}
