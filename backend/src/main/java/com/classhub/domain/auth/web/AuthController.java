package com.classhub.domain.auth.web;

import com.classhub.domain.auth.application.AuthService;
import com.classhub.domain.auth.dto.TeacherRegisterRequest;
import com.classhub.domain.auth.dto.TeacherRegisterResponse;
import com.classhub.global.response.RsCode;
import com.classhub.global.response.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/teacher")
    public RsData<TeacherRegisterResponse> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request
    ) {
        TeacherRegisterResponse response = authService.registerTeacher(request);
        return RsData.from(RsCode.CREATED, response);
    }
}
