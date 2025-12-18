package com.classhub.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.model.MemberRole;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * JwtProvider는 Security PLAN에서 정의한 토큰 생성·검증 계약을 담당하므로
 * 만료/Claim/Authentication 주입을 단위 테스트로 검증해둔다.
 */
class JwtProviderTest {

    @Test
    void generateAccessToken_shouldContainMemberIdRoleAndBeValid() {
        JwtProvider jwtProvider = new JwtProvider(defaultProperties());
        UUID memberId = UUID.randomUUID();
        MemberRole role = MemberRole.TEACHER;
        String token = jwtProvider.generateAccessToken(memberId, role);

        assertThat(jwtProvider.isValidToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(memberId);
        assertThat(jwtProvider.getAuthentication(token).getAuthorities())
                .extracting("authority")
                .containsExactly(role.name());

        MemberPrincipal principal = (MemberPrincipal) jwtProvider.getAuthentication(token).getPrincipal();
        assertThat(principal.id()).isEqualTo(memberId);
        assertThat(principal.role()).isEqualTo(role);
        assertThat(jwtProvider.getClaims(token).get("role", String.class)).isEqualTo(role.name());
    }

    @Test
    void generateRefreshToken_shouldBeValidAndOnlyContainMemberId() {
        JwtProvider jwtProvider = new JwtProvider(defaultProperties());
        UUID memberId = UUID.randomUUID();
        String refreshToken = jwtProvider.generateRefreshToken(memberId);

        assertThat(jwtProvider.isValidToken(refreshToken)).isTrue();
        assertThat(jwtProvider.getUserId(refreshToken)).isEqualTo(memberId);
        assertThat(jwtProvider.getClaims(refreshToken).get("role")).isNull();
        assertThat(jwtProvider.getClaims(refreshToken).get("authority")).isNull();
    }

    @Test
    void tokenShouldBecomeInvalidAfterExpiration() throws InterruptedException {
        JwtProperties jwtProperties = defaultProperties();
        jwtProperties.setAccessTokenExpirationMillis(1L);
        JwtProvider jwtProvider = new JwtProvider(jwtProperties);
        String token = jwtProvider.generateAccessToken(UUID.randomUUID(), MemberRole.TEACHER);

        TimeUnit.MILLISECONDS.sleep(10);

        assertThat(jwtProvider.isValidToken(token)).isFalse();
    }

    private JwtProperties defaultProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("classhub-test");
        properties.setSecretKey("0123456789012345678901234567890123456789012345678901234567890123");
        properties.setAccessTokenExpirationMillis(60_000L);
        properties.setRefreshTokenExpirationMillis(120_000L);
        return properties;
    }
}
