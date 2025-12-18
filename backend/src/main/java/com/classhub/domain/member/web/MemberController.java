package com.classhub.domain.member.web;

import com.classhub.domain.auth.dto.response.AuthTokens;
import com.classhub.domain.auth.dto.response.LoginResponse;
import com.classhub.domain.auth.support.RefreshTokenCookieProvider;
import com.classhub.domain.member.application.RegisterService;
import com.classhub.domain.member.dto.request.RegisterMemberRequest;
import com.classhub.domain.member.dto.request.RegisterStudentRequest;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Tag(name = "Member API", description = "회원 등록 및 정보 관련 API")
public class MemberController {

    private final RegisterService registerService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

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
}
