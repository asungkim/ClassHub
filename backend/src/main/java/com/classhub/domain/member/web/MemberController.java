package com.classhub.domain.member.web;

import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.LoginResponse;
import com.classhub.domain.auth.support.RefreshTokenCookieProvider;
import com.classhub.domain.member.application.MemberProfileService;
import com.classhub.domain.member.dto.MemberPrincipal;
import com.classhub.domain.member.dto.request.MemberProfileUpdateRequest;
import com.classhub.domain.member.application.RegisterService;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.dto.request.RegisterStudentRequest;
import com.classhub.domain.member.dto.response.MemberProfileResponse;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 등록 및 정보 관련 API")
public class MemberController {

    private final RegisterService registerService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final MemberProfileService memberProfileService;

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "로그인 사용자 기준으로 내 정보를 조회한다.")
    public RsData<MemberProfileResponse> getProfile(@AuthenticationPrincipal MemberPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        return RsData.from(RsCode.SUCCESS, memberProfileService.getProfile(principal.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "내 정보 수정", description = "로그인 사용자 기준으로 내 정보를 수정한다.")
    public RsData<MemberProfileResponse> updateProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody MemberProfileUpdateRequest request
    ) {
        if (principal == null) {
            throw new BusinessException(RsCode.UNAUTHENTICATED);
        }
        return RsData.from(RsCode.SUCCESS, memberProfileService.updateProfile(principal.id(), request));
    }

    @PostMapping("/register/teacher")
    @Operation(summary = "선생님 회원가입", description = "Teacher 역할 계정을 생성하고 Access/Refresh 토큰을 발급한다.")
    public RsData<LoginResponse> registerTeacher(
            @Valid @RequestBody RegisterMemberRequest request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = registerService.registerTeacher(request);
        refreshTokenCookieProvider.setRefreshToken(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return RsData.from(RsCode.SUCCESS, LoginResponse.from(tokens));
    }

    @PostMapping("/register/student")
    @Operation(summary = "학생 회원가입", description = "Student 역할 계정을 생성하고 StudentInfo까지 함께 저장한다.")
    public RsData<LoginResponse> registerStudent(
            @Valid @RequestBody RegisterStudentRequest request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = registerService.registerStudent(request);
        refreshTokenCookieProvider.setRefreshToken(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return RsData.from(RsCode.SUCCESS, LoginResponse.from(tokens));
    }

    @PostMapping("/register/assistant")
    @Operation(summary = "조교 회원가입", description = "조교 역할 계정을 생성하고 Access/Refresh 토큰을 발급한다.")
    public RsData<LoginResponse> registerAssistant(
            @Valid @RequestBody RegisterMemberRequest request,
            HttpServletResponse response
    ) {
        AuthTokens tokens = registerService.registerAssistant(request);
        refreshTokenCookieProvider.setRefreshToken(response, tokens.refreshToken(), tokens.refreshTokenExpiresAt());
        return RsData.from(RsCode.SUCCESS, LoginResponse.from(tokens));
    }
}
