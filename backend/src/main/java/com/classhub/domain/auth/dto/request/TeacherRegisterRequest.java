package com.classhub.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeacherRegisterRequest(
        @NotBlank
        @Email
        @Size(max = 120)
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-={}:;\"'`~<>,.?/\\\\|\\[\\]]).{8,64}$",
                message = "비밀번호는 8자 이상이며 영문/숫자/특수문자를 모두 포함해야 합니다."
        )
        String password,

        @NotBlank
        @Size(min = 1, max = 50)
        String name
) {
    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    public String sanitizedName() {
        return name.trim();
    }
}
