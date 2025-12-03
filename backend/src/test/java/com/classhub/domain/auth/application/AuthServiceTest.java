package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.request.LogoutRequest;
import com.classhub.domain.auth.dto.request.RefreshRequest;
import com.classhub.domain.auth.dto.request.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.response.LoginResponse;
import com.classhub.domain.auth.dto.response.TeacherRegisterResponse;
import com.classhub.domain.auth.token.InMemoryRefreshTokenStore;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "JWT_SECRET_KEY=0123456789012345678901234567890123456789012345678901234567890123")
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRefreshTokenStore refreshTokenStore;

    @BeforeEach
    void cleanUp() {
        memberRepository.deleteAll();
        refreshTokenStore.clear();
    }

    @Test
    @DisplayName("Teacher 계정을 생성하면 암호화된 패스워드와 Role=TEACHER로 저장된다")
    void registerTeacher_success() {
        TeacherRegisterRequest request = new TeacherRegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        );

        TeacherRegisterResponse response = authService.registerTeacher(request);

        assertThat(response.email()).isEqualTo("teacher@classhub.com");
        Member saved = memberRepository.findById(response.memberId()).orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.TEACHER);
        assertThat(passwordEncoder.matches("Classhub!1", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 BusinessException(DUPLICATE_EMAIL)을 던진다")
    void registerTeacher_duplicateEmail() {
        Member existing = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("선생님")
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.save(existing);

        TeacherRegisterRequest request = new TeacherRegisterRequest(
                "teacher@classhub.com",
                "Classhub!1",
                "김선생"
        );

        assertThatThrownBy(() -> authService.registerTeacher(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("올바른 자격 증명으로 로그인하면 Access/Refresh 토큰을 발급한다")
    void login_success() {
        Member member = createTeacher("teacher@classhub.com", "Classhub!1");
        LoginRequest request = new LoginRequest("teacher@classhub.com", "Classhub!1");

        LoginResponse response = authService.login(request);

        assertThat(response.memberId()).isEqualTo(member.getId());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.accessTokenExpiresAt()).isNotNull();
        assertThat(response.refreshTokenExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("비밀번호가 다르면 로그인에 실패한다")
    void login_invalidPassword() {
        createTeacher("teacher@classhub.com", "Classhub!1");
        LoginRequest request = new LoginRequest("teacher@classhub.com", "Wrong!2");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.UNAUTHENTICATED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void login_unknownEmail() {
        LoginRequest request = new LoginRequest("unknown@classhub.com", "Classhub!1");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.UNAUTHENTICATED.getMessage());
    }

    @Test
    @DisplayName("유효한 Refresh 토큰으로 Access/Refresh를 재발급한다")
    void refresh_success() {
        createTeacher("teacher@classhub.com", "Classhub!1");
        LoginResponse login = authService.login(new LoginRequest("teacher@classhub.com", "Classhub!1"));

        LoginResponse refreshed = authService.refresh(new RefreshRequest(login.refreshToken()));

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.memberId()).isEqualTo(login.memberId());
    }

    @Test
    @DisplayName("잘못된 Refresh 토큰이면 재발급을 거부한다")
    void refresh_invalidToken() {
        RefreshRequest request = new RefreshRequest("invalid.token.value");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.UNAUTHENTICATED.getMessage());
    }

    @Test
    @DisplayName("로그아웃하면 해당 Refresh 토큰으로 재발급을 차단한다")
    void logout_blacklistsToken() {
        createTeacher("teacher@classhub.com", "Classhub!1");
        LoginResponse login = authService.login(new LoginRequest("teacher@classhub.com", "Classhub!1"));

        authService.logout(new LogoutRequest(login.refreshToken(), false));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(login.refreshToken())))
                .isInstanceOf(BusinessException.class)
                .hasMessage(RsCode.UNAUTHENTICATED.getMessage());
    }

    private Member createTeacher(String email, String rawPassword) {
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .name("Teacher")
                .role(MemberRole.TEACHER)
                .build();
        return memberRepository.save(member);
    }
}
