package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.request.LogoutRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.MeResponse;
import com.classhub.domain.auth.token.RefreshTokenStore;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.jwt.JwtProvider;
import com.classhub.global.response.RsCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.normalizedEmail())
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));

        if (member.isDeleted()) {
            throw new BusinessException(RsCode.MEMBER_INACTIVE);
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }

        return issueTokens(member);
    }

    @Transactional(readOnly = true)
    public AuthTokens refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        if (refreshTokenStore.isBlacklisted(refreshToken)) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        if (!jwtProvider.isValidToken(refreshToken)) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }

        Member member = memberRepository.findById(jwtProvider.getUserId(refreshToken))
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));

        return issueTokens(member);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String token = request.refreshToken();
        if (token == null || !jwtProvider.isValidToken(token)) {
            return;
        }

        if (request.logoutAll()) {
            refreshTokenStore.blacklistAllForMember(jwtProvider.getUserId(token));
        } else if (!refreshTokenStore.isBlacklisted(token)) {
            refreshTokenStore.blacklist(token, jwtProvider.getExpiration(token));
        }
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));
        return MeResponse.from(member);
    }

    private AuthTokens issueTokens(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());
        LocalDateTime accessExpiresAt = jwtProvider.getExpiration(accessToken);
        LocalDateTime refreshExpiresAt = jwtProvider.getExpiration(refreshToken);

        return new AuthTokens(
                member.getId(),
                accessToken,
                accessExpiresAt,
                refreshToken,
                refreshExpiresAt
        );
    }
}
