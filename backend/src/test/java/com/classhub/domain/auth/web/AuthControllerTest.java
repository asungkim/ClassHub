package com.classhub.domain.auth.web;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = "JWT_SECRET_KEY=0123456789012345678901234567890123456789012345678901234567890123")
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitationRepository invitationRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        invitationRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("유효한 입력이면 Teacher 계정을 생성하고 요약 정보를 반환한다")
    void registerTeacher_success() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("teacher@classhub.com"))
                .andExpect(jsonPath("$.data.authority").value("TEACHER"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 DUPLICATE_EMAIL 코드로 실패한다")
    void registerTeacher_duplicateEmail() throws Exception {
        Member existing = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("선생님")
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.save(existing);

        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("비밀번호 정책을 만족하지 못하면 400을 반환한다")
    void registerTeacher_invalidPasswordPolicy() throws Exception {
        String payload = objectMapper.writeValueAsString(new RegisterRequest(
                "teacher2@classhub.com",
                "simplepw",
                "김선생"
        ));

        mockMvc.perform(post("/api/v1/auth/register/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 만료 정보가 포함된 응답을 반환한다")
    void login_success() throws Exception {
        createTeacher("teacher@classhub.com", "Classhub!1");
        String payload = objectMapper.writeValueAsString(new LoginPayload("teacher@classhub.com", "Classhub!1"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")));
    }

    @Test
    @DisplayName("잘못된 비밀번호면 401을 반환한다")
    void login_invalidPassword() throws Exception {
        createTeacher("teacher@classhub.com", "Classhub!1");
        String payload = objectMapper.writeValueAsString(new LoginPayload("teacher@classhub.com", "Wrong!2"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("LoginRequest Validation 실패 시 400을 반환한다")
    void login_validationFailure() throws Exception {
        String payload = objectMapper.writeValueAsString(new LoginPayload("invalid-email", ""));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("유효한 Access 토큰으로 /auth/me를 호출하면 현재 사용자 정보를 반환한다")
    void me_success() throws Exception {
        createTeacher("teacher@classhub.com", "Classhub!1");
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload("teacher@classhub.com", "Classhub!1"));
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("teacher@classhub.com"))
                .andExpect(jsonPath("$.data.name").value("선생님"))
                .andExpect(jsonPath("$.data.role").value("TEACHER"));
    }

    @Test
    @DisplayName("인증 정보 없이 /auth/me를 호출하면 401을 반환한다")
    void me_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("유효한 Refresh 토큰으로 Access/Refresh 재발급을 받는다")
    void refresh_success() throws Exception {
        createTeacher("teacher@classhub.com", "Classhub!1");
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload("teacher@classhub.com", "Classhub!1"));
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")));
    }

    @Test
    @DisplayName("Refresh 쿠키가 없으면 401을 반환한다")
    void refresh_invalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("초대 코드 검증 API는 inviter/role 정보를 반환한다")
    void verifyInvitation_success() throws Exception {
        Member teacher = createTeacher("teacher@classhub.com", "Classhub!1");
        Invitation invitation = createInvitation(teacher.getId(), "assistant@classhub.com", InvitationRole.ASSISTANT);
        String payload = objectMapper.writeValueAsString(Map.of("code", invitation.getCode()));

        mockMvc.perform(post("/api/v1/auth/invitations/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviterId").value(teacher.getId().toString()))
                .andExpect(jsonPath("$.data.inviteeRole").value("ASSISTANT"));
    }

    @Test
    @DisplayName("초대 코드 검증 실패 시 400을 반환한다")
    void verifyInvitation_invalid() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("code", "invalid"));

        mockMvc.perform(post("/api/v1/auth/invitations/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("초대 기반 회원가입 API는 Member 생성 후 토큰을 반환한다")
    void registerInvited_success() throws Exception {
        Member teacher = createTeacher("teacher@classhub.com", "Classhub!1");
        Invitation invitation = createInvitation(teacher.getId(), "assistant@classhub.com", InvitationRole.ASSISTANT);
        String payload = objectMapper.writeValueAsString(Map.of(
                "code", invitation.getCode(),
                "email", "assistant@classhub.com",
                "password", "Classhub!1",
                "name", "조교"
        ));

        mockMvc.perform(post("/api/v1/auth/register/invited")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")));
    }

    @Test
    @DisplayName("잘못된 초대 코드로 회원가입하면 400을 반환한다")
    void registerInvited_invalid() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "code", "wrong",
                "email", "assistant@classhub.com",
                "password", "Classhub!1",
                "name", "조교"
        ));

        mockMvc.perform(post("/api/v1/auth/register/invited")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("로그아웃 요청이 성공하면 200을 반환하고 해당 토큰은 재사용할 수 없다")
    void logout_success() throws Exception {
        createTeacher("teacher@classhub.com", "Classhub!1");
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload("teacher@classhub.com", "Classhub!1"));
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("쿠키 없이 로그아웃 요청을 보내도 200을 반환한다")
    void logout_withoutCookie() throws Exception {
        String payload = objectMapper.writeValueAsString(new LogoutPayload("", false));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    private Member createTeacher(String email, String rawPassword) {
        return memberRepository.save(Member.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name("선생님")
                .role(MemberRole.TEACHER)
                .build());
    }

    private Invitation createInvitation(UUID senderId, String email, InvitationRole role) {
        return invitationRepository.save(Invitation.builder()
                .senderId(senderId)
                .targetEmail(email)
                .inviteeRole(role)
                .code(UUID.randomUUID().toString())
                .expiredAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1))
                .build());
    }

    private record RegisterRequest(String email, String password, String name) {
    }

    private record LoginPayload(String email, String password) {
    }

    private record LogoutPayload(String refreshToken, boolean logoutAll) {
    }

}
