package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.LoginRequest;
import com.classhub.domain.auth.dto.LoginResponse;
import com.classhub.domain.auth.dto.RefreshRequest;
import com.classhub.domain.auth.dto.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.TeacherRegisterResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.jwt.JwtProperties;
import com.classhub.global.jwt.JwtProvider;
import com.classhub.global.response.RsCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final JwtProperties jwtProperties;

    @Transactional
    public TeacherRegisterResponse registerTeacher(TeacherRegisterRequest request) {
        String email = request.normalizedEmail();
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(RsCode.DUPLICATE_EMAIL);
        }

        Member teacher = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.sanitizedName())
                .role(MemberRole.TEACHER)
                .build();

        Member saved = memberRepository.save(teacher);
        return TeacherRegisterResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.normalizedEmail())
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }

        return issueTokens(member);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request) {
        String token = request.refreshToken();
        if (!jwtProvider.isValidToken(token)) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }

        Member member = memberRepository.findById(jwtProvider.getUserId(token))
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));

        return issueTokens(member);
    }

    private LoginResponse issueTokens(Member member) {
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime accessExpiresAt = now.plus(Duration.ofMillis(jwtProperties.getAccessTokenExpirationMillis()));
        LocalDateTime refreshExpiresAt = now.plus(Duration.ofMillis(jwtProperties.getRefreshTokenExpirationMillis()));

        return new LoginResponse(
                member.getId(),
                accessToken,
                refreshToken,
                accessExpiresAt,
                refreshExpiresAt
        );
    }
}
