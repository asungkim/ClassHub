package com.classhub.domain.auth.web;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.request.LoginRequest;
import com.classhub.domain.auth.dto.request.LogoutRequest;
import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.LoginResponse;
import com.classhub.domain.auth.dto.response.MeResponse;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.invitation.application.InvitationService;
import com.classhub.domain.invitation.dto.request.InvitationVerifyRequest;
import com.classhub.domain.invitation.dto.response.InvitationVerifyResponse;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import com.classhub.domain.auth.support.RefreshTokenCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "회원 가입/로그인/토큰 관련 API")
public class AuthController {

    private final AuthService authService;
    private final InvitationService invitationService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @GetMapping("/me")
    @Operation(summary = "현재 로그인한 사용자 정보 조회", description = "Access 토큰 기준으로 현재 사용자의 식별자/역할을 조회한다.")
    public RsData<MeResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        MeResponse response = authService.getCurrentMember(principal.id());
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 Access/Refresh 토큰을 발급한다.")
    public RsData<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthTokens tokens = authService.login(request);
        refreshTokenCookieProvider.setRefreshToken(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return RsData.from(RsCode.SUCCESS, LoginResponse.from(tokens));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh 토큰으로 Access 토큰을 재발급한다.")
    public RsData<LoginResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenCookieProvider.extractRefreshToken(request)
                .orElseThrow(() -> new BusinessException(RsCode.UNAUTHENTICATED));
        AuthTokens tokens = authService.refresh(refreshToken);
        refreshTokenCookieProvider.setRefreshToken(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return RsData.from(RsCode.SUCCESS, LoginResponse.from(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "갱신 토큰을 만료 처리한다.")
    public RsData<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        String refreshToken = refreshTokenCookieProvider.extractRefreshToken(httpServletRequest)
                .orElseGet(() -> request != null ? request.refreshToken() : null);
        if (refreshToken != null) {
            authService.logout(new LogoutRequest(refreshToken, request != null && request.logoutAll()));
        }
        refreshTokenCookieProvider.clearRefreshToken(httpServletResponse);
        return RsData.from(RsCode.SUCCESS, null);
    }
    @PostMapping("/invitations/verify")
    @Operation(summary = "초대 코드 검증", description = "초대 코드 유효성을 확인한다.")
    public RsData<InvitationVerifyResponse> verifyInvitation(
            @Valid @RequestBody InvitationVerifyRequest request
    ) {
        InvitationVerifyResponse response = invitationService.verifyCode(request);
        return RsData.from(RsCode.SUCCESS, response);
    }

}
