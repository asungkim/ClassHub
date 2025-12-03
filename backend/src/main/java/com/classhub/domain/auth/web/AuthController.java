package com.classhub.domain.auth.web;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.application.InvitationAuthService;
import com.classhub.domain.auth.dto.InvitationRegisterRequest;
import com.classhub.domain.auth.dto.InvitationVerifyRequest;
import com.classhub.domain.auth.dto.InvitationVerifyResponse;
import com.classhub.domain.auth.dto.LoginRequest;
import com.classhub.domain.auth.dto.LoginResponse;
import com.classhub.domain.auth.dto.LogoutRequest;
import com.classhub.domain.auth.dto.RefreshRequest;
import com.classhub.domain.auth.dto.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.TeacherRegisterResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final InvitationAuthService invitationAuthService;

    @PostMapping("/register/teacher")
    @Operation(summary = "Teacher 회원가입", description = "Teacher 계정을 등록한다.")
    public RsData<TeacherRegisterResponse> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request
    ) {
        TeacherRegisterResponse response = authService.registerTeacher(request);
        return RsData.from(RsCode.CREATED, response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 Access/Refresh 토큰을 발급한다.")
    public RsData<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh 토큰으로 Access 토큰을 재발급한다.")
    public RsData<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        LoginResponse response = authService.refresh(request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "갱신 토큰을 만료 처리한다.")
    public RsData<Void> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
        return RsData.from(RsCode.SUCCESS, null);
    }

    @PostMapping("/invitations/verify")
    @Operation(summary = "초대 코드 검증", description = "초대 코드 유효성을 확인한다.")
    public RsData<InvitationVerifyResponse> verifyInvitation(
            @Valid @RequestBody InvitationVerifyRequest request
    ) {
        InvitationVerifyResponse response = invitationAuthService.verify(request);
        return RsData.from(RsCode.SUCCESS, response);
    }

    @PostMapping("/register/invited")
    @Operation(summary = "초대 기반 회원가입", description = "초대 코드를 통한 회원가입을 처리한다.")
    public RsData<LoginResponse> registerInvited(
            @Valid @RequestBody InvitationRegisterRequest request
    ) {
        LoginResponse response = invitationAuthService.registerInvited(request);
        return RsData.from(RsCode.SUCCESS, response);
    }
}
