package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.request.LogoutRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.token.RefreshTokenStore;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.jwt.JwtProvider;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private AuthService authService;

    private Member teacher;
    private UUID teacherId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        teacher = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded-password")
                .name("Teacher Kim")
                .phoneNumber("01012345678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(teacher, "id", teacherId);
    }

    @Test
    void login_shouldIssueTokens_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("Teacher@classhub.com", "Classhub!1");
        given(memberRepository.findByEmail("teacher@classhub.com"))
                .willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("Classhub!1", teacher.getPassword()))
                .willReturn(true);

        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        LocalDateTime accessExpiresAt = LocalDateTime.now().plusMinutes(30);
        LocalDateTime refreshExpiresAt = LocalDateTime.now().plusDays(7);

        given(jwtProvider.generateAccessToken(teacherId, teacher.getRole()))
                .willReturn(accessToken);
        given(jwtProvider.generateRefreshToken(teacherId))
                .willReturn(refreshToken);
        given(jwtProvider.getExpiration(accessToken))
                .willReturn(accessExpiresAt);
        given(jwtProvider.getExpiration(refreshToken))
                .willReturn(refreshExpiresAt);

        AuthTokens tokens = authService.login(request);

        assertThat(tokens.memberId()).isEqualTo(teacherId);
        assertThat(tokens.accessToken()).isEqualTo(accessToken);
        assertThat(tokens.refreshToken()).isEqualTo(refreshToken);
        assertThat(tokens.accessTokenExpiresAt()).isEqualTo(accessExpiresAt);
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(refreshExpiresAt);
    }

    @Test
    void login_shouldThrow_whenMemberIsDeleted() {
        teacher.deactivate();
        LoginRequest request = new LoginRequest("teacher@classhub.com", "Classhub!1");
        given(memberRepository.findByEmail("teacher@classhub.com"))
                .willReturn(Optional.of(teacher));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.MEMBER_INACTIVE);
    }

    @Test
    void login_shouldThrow_whenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest("teacher@classhub.com", "wrong");
        given(memberRepository.findByEmail("teacher@classhub.com"))
                .willReturn(Optional.of(teacher));
        given(passwordEncoder.matches("wrong", teacher.getPassword()))
                .willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.UNAUTHENTICATED);
    }

    @Test
    void logout_shouldBlacklistToken_whenSingleDeviceLogout() {
        String refreshToken = "refresh-token";
        LogoutRequest request = new LogoutRequest(refreshToken, false);
        LocalDateTime expiration = LocalDateTime.now().plusDays(7);

        given(jwtProvider.isValidToken(refreshToken)).willReturn(true);
        given(refreshTokenStore.isBlacklisted(refreshToken)).willReturn(false);
        given(jwtProvider.getExpiration(refreshToken)).willReturn(expiration);

        authService.logout(request);

        verify(refreshTokenStore).blacklist(refreshToken, expiration);
        verify(refreshTokenStore, never()).blacklistAllForMember(any());
    }

    @Test
    void logout_shouldBlacklistAllTokens_whenLogoutAll() {
        String refreshToken = "refresh-token";
        LogoutRequest request = new LogoutRequest(refreshToken, true);
        UUID memberId = UUID.randomUUID();

        given(jwtProvider.isValidToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(memberId);

        authService.logout(request);

        verify(refreshTokenStore).blacklistAllForMember(memberId);
        verify(refreshTokenStore, never()).blacklist(any(), any());
    }

    @Test
    void logout_shouldDoNothing_whenTokenInvalid() {
        LogoutRequest request = new LogoutRequest("invalid", false);
        given(jwtProvider.isValidToken("invalid")).willReturn(false);

        authService.logout(request);

        verify(refreshTokenStore, never()).blacklist(any(), any());
        verify(refreshTokenStore, never()).blacklistAllForMember(any());
    }
}
